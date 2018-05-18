package com.virjar.dungproxy.newserver.storage;

import com.google.common.base.Preconditions;
import com.virjar.dungproxy.newserver.util.PathResolver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by virjar on 2018/5/18.<br>
 * 存储代理ip资源,由于上一个版本存储在mysql中,其查询性能收到影响,即使命中索引也无法在一个比较不太好的服务器上面良好运行,所以考虑自己实现文件存储
 */
public class IPResourceStorage {
    private static final String dataPath = "file:~/.dungproxy_server/proxyData.proxydb";
    private static final long fileLimitLength = 1 << 18;
    private MappedByteBuffer mappedByteBuffer = null;
    private int maxDataSize = -1;

    //magic字段,11个字节
    private byte[] magic = "dungproxydb".getBytes();
    //当前记录大小
    private int recordSize = 0;
    //16个字节,用来存储文件头部信息
    private int headerSize = magic.length + 4;

    private DataNode rootNode = null;

    public IPResourceStorage() {
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
            if (!dataFile.createNewFile()) {
                throw new IOException("failed to create data file:" + dataFile.getAbsolutePath());
            }
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(dataFile, "rwd");
        FileChannel fc = randomAccessFile.getChannel();
        mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fileLimitLength);
        maxDataSize = (int) ((fileLimitLength - headerSize) / DataNode.dataSize);
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
    }

    private void createNewModel() {
        //write magic header
        writeBytes(mappedByteBuffer, 0, magic);
        writeInt(mappedByteBuffer, magic.length, 0);
    }


    private static class DataNode {
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
        private int treeSize;

        static final int dataSize = 4 + 4 + 8 + 4 + 4 + 4;
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

}
