package com.virjar.dungproxy.newserver.storage;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.virjar.dungproxy.newserver.util.PathResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by virjar on 2018/5/18.<br>
 * 存储代理ip资源,由于上一个版本存储在mysql中,其查询性能受到影响,即使命中索引也无法在一个比较不太好的服务器上面良好运行,所以考虑自己实现文件存储
 */
@Slf4j
public class IPResourceStorage implements Iterable<Long> {
    private static final String dataPath = "file:~/.dungproxy_server/proxyData.proxydb";
    //这会产生一个256M的文件
    private static final long fileLimitLength = 1 << 28;
    private MappedByteBuffer mappedByteBuffer = null;
    private int maxDataSize = -1;

    //magic字段,11个字节
    private byte[] magic = "dungproxydb".getBytes();
    //当前记录大小
    private int recordSize = 0;
    private volatile int spaceStartIndex = 0;
    //16个字节,用来存储文件头部信息
    private int headerSize = magic.length + 4 + 4 + 4;
    private final int dataSize = calculateDataNodeSize();
    private int firstBucketIndex = 0;
    private LoadingCache<Integer, DataNode> dataNodeCache = CacheBuilder.newBuilder()
            //缓存2048个数据节点在内存
            .maximumSize(1 << 11).build(new CacheLoader<Integer, DataNode>() {
                @Override
                public DataNode load(Integer key) throws Exception {
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
            writeHeader();
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
        firstBucketIndex = mappedByteBuffer.getInt();
        scanFreeBucket();
    }

    private void scanFreeBucket() {
        if (recordSize == spaceStartIndex) {
            return;
        }
        int i = 0;
        while (freeBucket.size() + recordSize < spaceStartIndex) {
            if (i > spaceStartIndex) {
                log.warn("data maybe broken");
                return;
            }
            DataNode dataNode = dataNodeCache.getUnchecked(i);
            Preconditions.checkNotNull(dataNode);
            if ((dataNode.flags & maskUsed) == 0) {
                freeBucket.add(i);
            }
            i++;
        }
    }

    private int nextAvailableBucket() {
        Integer ret = freeBucket.pollFirst();
        if (ret == null) {
            ret = spaceStartIndex++;
        }
        dataNodeCache.invalidate(ret);
        return ret;
    }

    private void writeHeader() {
        //write magic header
        writeBytes(mappedByteBuffer, 0, magic);
        mappedByteBuffer.putInt(recordSize);
        mappedByteBuffer.putInt(spaceStartIndex);
        mappedByteBuffer.putInt(firstBucketIndex);
    }

    public long getByIndex(int index) {
        Preconditions.checkPositionIndex(index, recordSize);
        readWriteLock.readLock().lock();
        try {
            DataNode parentNode = dataNodeCache.getUnchecked(firstBucketIndex);
            int baseSize = 0;
            while (true) {
                Preconditions.checkNotNull(parentNode);
                if (baseSize + parentNode.leftDataSize < index) {
                    //数据在右边
                    baseSize += (parentNode.leftDataSize + 1);
                    parentNode = dataNodeCache.getUnchecked(parentNode.rightChildOffset);
                    continue;
                }
                if (baseSize + parentNode.leftDataSize > index) {
                    //数据在左子树
                    parentNode = dataNodeCache.getUnchecked(parentNode.leftChildOffset);
                    Preconditions.checkNotNull(parentNode);
                    baseSize += (parentNode.leftDataSize + 1);
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
            DataNode parentNode = dataNodeCache.getUnchecked(firstBucketIndex);
            int baseSize = 0;
            while (true) {
                Preconditions.checkNotNull(parentNode);
                if (baseSize + parentNode.leftDataSize < index) {
                    //数据在右边
                    baseSize += (parentNode.leftDataSize + 1);
                    parentNode = dataNodeCache.getUnchecked(parentNode.rightChildOffset);
                    //Preconditions.checkNotNull(parentNode);
                    continue;
                }
                if (baseSize == index) {
                    //命中当前节点
                    if (index == 0 && parentNode.leftDataSize <= 0) {
                        removeNode(parentNode);
                        return parentNode.theData;
                    }
                    return rightShift(parentNode);
                }
                //数据在左子树
                parentNode = dataNodeCache.getUnchecked(parentNode.leftChildOffset);
                Preconditions.checkNotNull(parentNode);
                baseSize += (parentNode.leftDataSize + 1);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void removeNode(DataNode dataNode) {
        recordSize--;
        if (recordSize == 0) {
            firstBucketIndex = 0;
        } else {
            firstBucketIndex = dataNode.rightChildOffset;
        }
        dataNode.parentIndex = -1;
        dataNode.leftDataSize = 0;
        dataNode.rightDataSize = 0;
        dataNode.flags &= unMaskUsed;
        dataNode.flush();
        writeHeader();
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

    public void clear() {
        recordSize = 0;
        spaceStartIndex = 0;
        firstBucketIndex = 0;
        writeHeader();
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
                writeHeader();
                return;
            }
            recordSize++;
            DataNode parentNode = dataNodeCache.getUnchecked(firstBucketIndex);
            int baseSize = 0;
            while (true) {
                Preconditions.checkNotNull(parentNode);
                // parentNode.treeSize++;
                if (baseSize + parentNode.leftDataSize < index) {
                    //数据添加在左子树
                    baseSize += (parentNode.leftDataSize + 1);
                    // 父节点左边没有数据,新增左子树
                    if (parentNode.leftDataSize <= 0) {
                        attachParent(parentNode, dataNode, true);
                        writeHeader();
                        return;
                    }
                    // 父节点右边没有数据,新增右子树
                    if (parentNode.rightDataSize <= 0) {
                        attachParent(parentNode, dataNode, false);
                        writeHeader();
                        return;
                    }

                    parentNode.rightDataSize++;
                    parentNode.flush();
                    // 设置右子树为父节点
                    parentNode = dataNodeCache.getUnchecked(parentNode.rightChildOffset);
                    continue;
                }
                if (baseSize + parentNode.leftDataSize == index) {
                    //目标命中当前节点,需要迁移当前节点。左子树左移
                    leftShift(parentNode);
                    parentNode.leftDataSize++;
                    parentNode.theData = data;
                    parentNode.flush();
                    return;
                }
                parentNode.flush();
                //目标在左子树
                parentNode = dataNodeCache.getUnchecked(parentNode.leftChildOffset);
                Preconditions.checkNotNull(parentNode);
                baseSize += (parentNode.leftDataSize + 1);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void attachParent(DataNode parentNode, DataNode child, boolean left) {
        child.parentIndex = parentNode.dataIndex;
        child.dataIndex = nextAvailableBucket();
        if (left) {
            parentNode.leftDataSize++;
            parentNode.leftChildOffset = child.dataIndex;
        } else {
            parentNode.rightChildOffset = child.dataIndex;
            parentNode.rightDataSize++;
        }
        parentNode.flush();
        child.flush();
    }

    private long rightShift(DataNode dataNode) {
        long theData = dataNode.theData;
        while (true) {
            if (dataNode.leftDataSize <= 0) {
                dataNode.flags &= unMaskUsed;
                recordSize--;
                dataNode.leftDataSize = 0;
                dataNode.rightDataSize = 0;
                dataNode.flush();
                freeBucket.add(dataNode.dataIndex);
                writeHeader();
                return theData;
            }
            dataNode.leftDataSize--;
            DataNode ifPresent = dataNodeCache.getUnchecked(dataNode.leftChildOffset);
            Preconditions.checkNotNull(ifPresent);
            dataNode.theData = ifPresent.theData;
            dataNode.flush();
            dataNode = ifPresent;
        }
    }

    private void leftShift(DataNode dataNode) {
        long theData = dataNode.theData;
        while (true) {
            if (dataNode.leftDataSize <= 0) {
                //create a new node
                DataNode newDataNode = new DataNode();
                newDataNode.parentIndex = dataNode.dataIndex;
                newDataNode.dataIndex = nextAvailableBucket();
                newDataNode.theData = theData;
                newDataNode.flags |= maskUsed;
                dataNode.flush();
                newDataNode.flush();
                writeHeader();
                return;
            }
            // dataNode.treeSize++;
            dataNode.leftDataSize++;
            dataNode.flush();

            dataNode = dataNodeCache.getUnchecked(dataNode.leftChildOffset);
            Preconditions.checkNotNull(dataNode);
            long tmpData = dataNode.theData;
            dataNode.theData = theData;
            theData = tmpData;
        }
    }


    private class DataNode {
        //4个字节的空间,已经能够描述过亿的数据量了
        //左节点的节点总数,4个字节描述
        private int leftDataSize = 0;
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
        private byte flags = 0;

        //以下为描述字段,不参与序列化
        //当前节点的索引值
        private int dataIndex;


        void flush() {
            readWriteLock.writeLock().lock();
            try {
                int start = headerSize + dataSize * dataIndex;
                flushDataNode(mappedByteBuffer, start, this);
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
        mappedByteBuffer.putInt(dataNode.leftDataSize);
        mappedByteBuffer.putInt(dataNode.leftChildOffset);
        mappedByteBuffer.putLong(dataNode.theData);
        mappedByteBuffer.putInt(dataNode.rightDataSize);
        mappedByteBuffer.putInt(dataNode.rightChildOffset);
        //make sure node container data size is right
        //int treeSize = dataNode.rightDataSize + dataNode.leftDataSize + 1;
        //mappedByteBuffer.putInt(treeSize);
        mappedByteBuffer.putInt(dataNode.parentIndex);
        mappedByteBuffer.put(dataNode.flags);
    }


    private DataNode read(int index) {
        readWriteLock.readLock().lock();
        try {
            int start = headerSize + dataSize * index;
            mappedByteBuffer.position(start);
            DataNode dataNode = new DataNode();
            dataNode.leftDataSize = mappedByteBuffer.getInt();
            dataNode.leftChildOffset = mappedByteBuffer.getInt();
            dataNode.theData = mappedByteBuffer.getLong();
            dataNode.rightDataSize = mappedByteBuffer.getInt();
            dataNode.rightChildOffset = mappedByteBuffer.getInt();
            //dataNode.treeSize = mappedByteBuffer.getInt();
            dataNode.dataIndex = index;
            dataNode.parentIndex = mappedByteBuffer.getInt();
            dataNode.flags = mappedByteBuffer.get();
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

    public int size() {
        return recordSize;
    }

    @Override
    public Iterator<Long> iterator() {
        return new InnerIterator();
    }

    private class InnerIterator implements Iterator<Long> {
        private int nextIndex;
        private int nowIndex = -1;

        InnerIterator() {
            if (recordSize <= 0) {
                nextIndex = -1;
            } else {
                nextIndex = firstBucketIndex;
            }
        }

        @Override
        public boolean hasNext() {
            return nextIndex >= 0;
        }

        @Override
        public Long next() {
            nowIndex = nextIndex;
            DataNode dataNode = dataNodeCache.getUnchecked(nextIndex);
            if (dataNode.leftDataSize > 0) {
                nextIndex = dataNode.leftChildOffset;
                return dataNode.theData;
            }
            if (dataNode.rightDataSize > 0) {
                nextIndex = dataNode.rightChildOffset;
                return dataNode.theData;
            }
            Preconditions.checkArgument(dataNode.parentIndex >= 0);
            DataNode preDataNode = dataNodeCache.getUnchecked(dataNode.parentIndex);
            while (true) {
                if (preDataNode.leftDataSize > 0) {
                    nextIndex = preDataNode.rightChildOffset;
                    return dataNode.theData;
                }
                if (preDataNode.parentIndex < 0) {
                    nextIndex = -1;
                    return dataNode.theData;
                }
                preDataNode = dataNodeCache.getUnchecked(preDataNode.parentIndex);
            }
        }

        @Override
        public void remove() {
            Preconditions.checkArgument(nowIndex > 0, "cursor not setting");
            DataNode dataNode = dataNodeCache.getUnchecked(nowIndex);
            if (dataNode.parentIndex < 0) {
                removeNode(dataNode);
                return;
            }
            rightShift(dataNode);
        }

    }

    /**
     * print all node on the  console,an useful tool for test,
     * 注意,最好测试数据都是个位数
     */
    public void printTree() {
        List<List<Long>> lines = Lists.newLinkedList();
        printTreeInternal(0, 0, lines, firstBucketIndex);
        int totalWith = 1 << (lines.size() - 1);
        for (int i = 0; i < lines.size(); i++) {
            List<Long> line = lines.get(i);
            StringBuilder sb = new StringBuilder(totalWith);
            for (Long value : line) {
                sb.append(StringUtils.center(value < 0 ? String.valueOf("X") : String.valueOf(value), 1 << (lines.size() - i), " "));
            }
            for (int j = line.size(); j < (1 << i); j++) {
                sb.append(StringUtils.center("X", 1 << (lines.size() - i), " "));
            }
            System.out.println(sb.toString());
        }
    }

    private void printTreeInternal(int deep, int offset, List<List<Long>> lines, int nodeIndex) {
        DataNode dataNode = dataNodeCache.getUnchecked(nodeIndex);
        List<Long> line;
        if (lines.size() <= deep) {
            line = Lists.newArrayListWithCapacity(2 ^ deep);
            lines.add(deep, line);
        } else {
            line = lines.get(deep);
        }
        for (int i = line.size(); i < offset; i++) {
            line.add(-1L);
        }
        line.add(offset, dataNode.theData);
        if (dataNode.leftDataSize > 0) {
            printTreeInternal(deep + 1, offset * 2, lines, dataNode.leftChildOffset);
        }
        if (dataNode.rightDataSize > 0) {
            printTreeInternal(deep + 1, offset * 2 + 1, lines, dataNode.rightChildOffset);
        }
    }

    public static void main(String[] args) {
        IPResourceStorage ipResourceStorage = new IPResourceStorage();
        for (int i = 0; i < 11; i++) {
            ipResourceStorage.offer(i);
        }
        ipResourceStorage.printTree();
        ipResourceStorage.clear();
    }
}
