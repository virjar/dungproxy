package com.virjar.dungproxy.newserver.storage;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Queues;
import com.virjar.dungproxy.newserver.util.PathResolver;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by virjar on 2018/5/18.<br>
 * 存储代理ip资源,由于上一个版本存储在mysql中,其查询性能收到影响,即使命中索引也无法在一个比较不太好的服务器上面良好运行,所以考虑自己实现文件存储
 */
@Slf4j
public class IPResourceStorage {
    private static final String dataPath = "file:~/.dungproxy_server/proxyData.proxydb";
    private static final long fileLimitLength = 1 << 18;
    private MappedByteBuffer mappedByteBuffer = null;
    private int maxDataSize = -1;

    //magic字段,11个字节
    private byte[] magic = "dungproxydb".getBytes();
    //当前记录大小
    private int recordSize = 0;
    private volatile int spaceStartIndex = 0;
    //16个字节,用来存储文件头部信息
    private int headerSize = magic.length + 4;
    private final int dataSize = calculateDataNodeSize();
    private Cache<Integer, DataNode> dataNodeCache = CacheBuilder.newBuilder()
            //缓存2048个数据节点在内存
            .maximumSize(1 << 11).build(new CacheLoader<Integer, DataNode>() {
                @Override
                public DataNode load(Integer key) throws Exception {
                    if (recordSize <= 0) {
                        return null;
                    }
                    return read(key);
                }
            });
    private LinkedBlockingDeque<Integer> freeBucket = Queues.newLinkedBlockingDeque();

    //读写锁,该队列暂时没有实现分块锁,目前锁整个队列
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private static final int maskUsed = 0x01;
    private static final int unMaskUsed = ~maskUsed;


    public IPResourceStorage() {
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException("failed to init db file", e);
        }
    }

    private boolean hasInit = false;

    private synchronized void init() throws IOException {
        if (hasInit) {
            return;
        }

        boolean isNewFile = false;
        File dataFile = new File(PathResolver.resolveAbsolutePath(dataPath));
        if (!dataFile.exists()) {
            isNewFile = true;
            File parentFile = dataFile.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                throw new IOException("failed to create data file:" + dataFile.getAbsolutePath());
            }
            if (!dataFile.createNewFile()) {
                throw new IOException("failed to create data file:" + dataFile.getAbsolutePath());
            }
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(dataFile, "rwd");
        FileChannel fc = randomAccessFile.getChannel();
        mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fileLimitLength);
        maxDataSize = (int) ((fileLimitLength - headerSize) / dataSize);
        if (isNewFile) {
            createNewModel();
        } else {
            parseModel();
        }

        hasInit = true;
    }

    private void parseModel() {
        mappedByteBuffer.position(0);
        byte[] magicBuffer = new byte[magic.length];
        mappedByteBuffer.get(magicBuffer);
        for (int i = 0; i < magic.length; i++) {
            Preconditions.checkArgument(magicBuffer[i] == magic[i], "broken dungproxy queue db file");
        }
        recordSize = mappedByteBuffer.getInt();
        spaceStartIndex = mappedByteBuffer.getInt();
        scanFreeBucket();
    }

    private void scanFreeBucket() {
        if (recordSize == spaceStartIndex) {
            return;
        }
        int i = 0;
        while (freeBucket.size() + recordSize <= spaceStartIndex) {
            if (i > spaceStartIndex) {
                log.warn("data maybe broken");
                return;
            }
            DataNode dataNode = dataNodeCache.getIfPresent(i);
            Preconditions.checkNotNull(dataNode);
            if ((dataNode.flags & maskUsed) != 0) {
                freeBucket.add(i);
            }
            i++;
        }
    }

    private int nextAvailableBucket() {
        Integer integer = freeBucket.pollFirst();
        if (integer == null) {
            spaceStartIndex++;
            return spaceStartIndex;
        }
        return integer;
    }

    private void createNewModel() {
        //write magic header
        writeBytes(mappedByteBuffer, 0, magic);
        writeInt(mappedByteBuffer, magic.length, 0);
        writeInt(mappedByteBuffer, magic.length + 4, 0);
    }

    public long getByIndex(int index) {
        Preconditions.checkPositionIndex(index, recordSize);
        readWriteLock.readLock().lock();
        try {
            DataNode parentNode = dataNodeCache.getIfPresent(0);
            int baseSize = 0;
            while (true) {
                Preconditions.checkNotNull(parentNode);
                if (baseSize + parentNode.leftDateSize < index) {
                    //数据在右边
                    baseSize += (parentNode.leftDateSize + 1);
                    parentNode = dataNodeCache.getIfPresent(parentNode.rightChildOffset);
                    continue;
                }
                if (baseSize + parentNode.leftDateSize > index) {
                    //数据在左子树
                    parentNode = dataNodeCache.getIfPresent(parentNode.leftChildOffset);
                    Preconditions.checkNotNull(parentNode);
                    baseSize += (parentNode.leftDateSize + 1);
                    continue;
                }
                //命中当前节点
                return parentNode.theData;
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public long removeByIndex(int index) {
        Preconditions.checkPositionIndex(index, recordSize);
        readWriteLock.writeLock().lock();
        try {
            DataNode parentNode = dataNodeCache.getIfPresent(0);
            int baseSize = 0;
            while (true) {
                Preconditions.checkNotNull(parentNode);
                if (baseSize + parentNode.leftDateSize < index) {
                    //数据在右边
                    baseSize += (parentNode.leftDateSize + 1);
                    parentNode = dataNodeCache.getIfPresent(parentNode.rightChildOffset);
                    //Preconditions.checkNotNull(parentNode);
                    continue;
                }
                if (baseSize + parentNode.leftDateSize > index) {
                    //数据在左子树
                    parentNode = dataNodeCache.getIfPresent(parentNode.leftChildOffset);
                    Preconditions.checkNotNull(parentNode);
                    baseSize += (parentNode.leftDateSize + 1);
                    continue;
                }
                //命中当前节点
                return rightShift(parentNode);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void offer(long data) {
        addIndex(data, recordSize);
    }

    public long poll() {
        if (recordSize == 0) {
            return -1;
        }
        return removeByIndex(0);
    }

    /**
     * 插入数据到指定index的前面
     *
     * @param data  数据
     * @param index 插入位置
     */
    public void addIndex(long data, int index) {
        Preconditions.checkPositionIndex(index, recordSize);
        Preconditions.checkArgument(index < maxDataSize, "max data record limited,max" + maxDataSize + ",data index:" + index);
        DataNode dataNode = new DataNode();
        dataNode.theData = data;
        dataNode.flags |= maskUsed;
        readWriteLock.writeLock().lock();
        try {
            if (recordSize == 0) {
                //the first node
                dataNode.parentIndex = -1;
                dataNode.dataIndex = nextAvailableBucket();
                // dataNode.treeSize = 1;
                recordSize++;
                dataNode.flush();
                return;
            }
            recordSize++;
            DataNode parentNode = dataNodeCache.getIfPresent(0);
            int baseSize = 0;
            while (true) {
                Preconditions.checkNotNull(parentNode);
                // parentNode.treeSize++;
                if (baseSize + parentNode.leftDateSize < index) {
                    //数据添加在右子树
                    baseSize += (parentNode.leftDateSize + 1);

                    if (parentNode.rightChildOffset < 0 && parentNode.leftChildOffset < 0) {
                        //左右子树都为空,该节点为叶节点,需要随机选择增加二叉树平衡的概率
                        attachParent(parentNode, dataNode, ThreadLocalRandom.current().nextBoolean());
                        return;
                    }
                    if (parentNode.rightChildOffset < 0) {
                        attachParent(parentNode, dataNode, false);
                        return;
                    }
                    if (parentNode.leftChildOffset < 0) {
                        attachParent(parentNode, dataNode, true);
                        return;
                    }
                    parentNode.flush();
                    parentNode = dataNodeCache.getIfPresent(parentNode.leftChildOffset);
                    continue;
                }
                if (baseSize + parentNode.leftDateSize == index) {
                    //目标命中当前节点,需要迁移当前节点。左子树左移
                    leftShift(parentNode);
                    parentNode.leftDateSize++;
                    parentNode.theData = data;
                    parentNode.flush();
                    return;
                }
                parentNode.flush();
                //目标在左子树
                parentNode = dataNodeCache.getIfPresent(parentNode.leftChildOffset);
                Preconditions.checkNotNull(parentNode);
                baseSize += (parentNode.leftDateSize + 1);


            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void attachParent(DataNode parentNode, DataNode child, boolean left) {
        if (left) {
            parentNode.leftDateSize++;
            parentNode.leftChildOffset = spaceStartIndex++;
        } else {
            parentNode.rightDataSize++;
            parentNode.rightChildOffset = spaceStartIndex++;
        }
        child.parentIndex = parentNode.dataIndex;
        child.dataIndex = nextAvailableBucket();
        parentNode.flush();
        child.flush();
    }

    private long rightShift(DataNode dataNode) {
        long theData = dataNode.theData;
        while (true) {
            if (dataNode.leftChildOffset < 0) {
                dataNode.flags &= unMaskUsed;
                dataNode.flush();
                freeBucket.add(dataNode.dataIndex);
                return theData;
            }
            dataNode.leftDateSize--;
            DataNode ifPresent = dataNodeCache.getIfPresent(dataNode.leftChildOffset);
            Preconditions.checkNotNull(ifPresent);
            dataNode.theData = ifPresent.theData;
            dataNode.flush();
            dataNode = ifPresent;
        }
    }

    private void leftShift(DataNode dataNode) {
        long theData = dataNode.theData;
        while (true) {
            if (dataNode.leftChildOffset < 0) {
                //create a new node
                DataNode newDataNode = new DataNode();
                newDataNode.parentIndex = dataNode.dataIndex;
                newDataNode.dataIndex = nextAvailableBucket();
                newDataNode.theData = theData;
                newDataNode.flags |= maskUsed;
                dataNode.flush();
                newDataNode.flush();
                return;
            }
            // dataNode.treeSize++;
            dataNode.leftDateSize++;
            dataNode.flush();

            dataNode = dataNodeCache.getIfPresent(dataNode.leftChildOffset);
            Preconditions.checkNotNull(dataNode);
            long tmpData = dataNode.theData;
            dataNode.theData = theData;
            theData = tmpData;
        }
    }


    private int lastRecordSizeRecord = -1;

    private class DataNode {
        //4个字节的空间,已经能够描述过亿的数据量了
        //左节点的节点总数,4个字节描述
        private int leftDateSize = 0;
        //左节点offset,如果不存在左节点,则为-1。4个字节描述
        private int leftChildOffset = -1;
        //数据区域,这里指代ip,8个字节
        private long theData;
        //右节点的节点总数,4个字节描述
        private int rightDataSize = 0;
        //右节点offset,如果不存在左节点,则为-1。4个字节描述
        private int rightChildOffset = -1;
        //以本节点作为根节点的存储树的数据总量
        //private int treeSize;
        //父节点索引值
        private int parentIndex;
        //数据段描述信息
        private int flags = 0;

        //以下为描述字段,不参与序列化
        //当前节点的索引值
        private int dataIndex;


        void flush() {
            readWriteLock.writeLock().lock();
            try {
                int start = headerSize + dataSize * dataIndex;
                flushDataNode(mappedByteBuffer, start, this);
                if (lastRecordSizeRecord == recordSize) {
                    return;
                }
                //尽量避免访问磁盘
                mappedByteBuffer.position(magic.length);
                mappedByteBuffer.putInt(recordSize);
                mappedByteBuffer.putInt(spaceStartIndex);
                lastRecordSizeRecord = recordSize;
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }

    }

    private int calculateDataNodeSize() {
        MappedByteBuffer mappedByteBuffer = (MappedByteBuffer) MappedByteBuffer.allocateDirect(1024);
        DataNode dataNode = new DataNode();
        flushDataNode(mappedByteBuffer, 0, dataNode);
        return mappedByteBuffer.position();
    }

    private static void flushDataNode(MappedByteBuffer mappedByteBuffer, int offset, DataNode dataNode) {
        mappedByteBuffer.position(offset);
        mappedByteBuffer.putInt(dataNode.leftDateSize);
        mappedByteBuffer.putInt(dataNode.leftChildOffset);
        mappedByteBuffer.putLong(dataNode.theData);
        mappedByteBuffer.putInt(dataNode.rightDataSize);
        mappedByteBuffer.putInt(dataNode.rightChildOffset);
        //make sure node container data size is right
        //int treeSize = dataNode.rightDataSize + dataNode.leftDateSize + 1;
        //mappedByteBuffer.putInt(treeSize);
        mappedByteBuffer.putInt(dataNode.parentIndex);
    }


    private DataNode read(int index) {
        readWriteLock.readLock().lock();
        try {
            int start = headerSize + dataSize * index;
            mappedByteBuffer.position(start);
            DataNode dataNode = new DataNode();
            dataNode.leftDateSize = mappedByteBuffer.getInt();
            dataNode.leftChildOffset = mappedByteBuffer.getInt();
            dataNode.theData = mappedByteBuffer.getLong();
            dataNode.rightDataSize = mappedByteBuffer.getInt();
            dataNode.rightChildOffset = mappedByteBuffer.getInt();
            //dataNode.treeSize = mappedByteBuffer.getInt();
            dataNode.dataIndex = index;
            dataNode.parentIndex = mappedByteBuffer.getInt();
            return dataNode;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    //tool function
    private void writeInt(MappedByteBuffer mappedByteBuffer, int offset, int data) {
        mappedByteBuffer.putInt(offset, data);
//        byte[] encodeData = new byte[4];
//        encodeData[0] = (byte) (data >>> 24 & 0xFF);
//        encodeData[1] = (byte) (data >>> 16 & 0xFF);
//        encodeData[2] = (byte) (data >>> 8 & 0xFF);
//        encodeData[1] = (byte) (data & 0xFF);
//        writeBytes(mappedByteBuffer, offset, encodeData);
    }

    private void writeBytes(MappedByteBuffer mappedByteBuffer, int offset, byte[] data) {
        Preconditions.checkArgument(offset + data.length < fileLimitLength, "dataStorage limited,please increment max file length settings");
        mappedByteBuffer.position(offset);
        mappedByteBuffer.put(data);
        //原生api中,offset指代data中的offset,而非MappedByteBuffer中的offset,所以不能使用原生api
    }

    public static void main(String[] args) {
        IPResourceStorage ipResourceStorage = new IPResourceStorage();
        for (int i = 0; i < 6; i++) {
            ipResourceStorage.offer(i);
        }
        for (int i = 0; i < 6; i++) {
            System.out.println(ipResourceStorage.poll());
        }

    }
}
