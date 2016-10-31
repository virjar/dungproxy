package com.virjar.dungproxy.server.distributer;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.model.ProxyModel;

/**
 * 标示已经被分配过的IP,使用bloomFilter的思想,传递一个签名 Created by virjar on 16/8/31. 该容器
 */
public class DistributedSign {
    // 每个签名容器最多存500个IP使用信息,如果超过,服务器将会强制重置
    private static final int maxNumber = 300;
    // hash标记位,一般来说这个数值越高越不容易出现数据冲突,但是相对来说容器大小将会增加。服务器使用的是22个,考虑客户端数据量较小,设置为10
    private static final int hashFunction = 5;
    // 随机种子,我们这里只是使用一个种子产生数字序列,用来散列数据,所以这个数值作为客户端服务器协议的一部分,共同约定即可,无特殊含义
    private static final int seed = 516;
    final public static int NUMBER_OF_WEIGHTS = 2083; // CHANGED FROM 16
    /** The underlying bit vectorS. */
    private int[] bits;
    /** The random integers used to generate the hash functions. */
    private int[][] weight;

    private long totalBits;

    /** The natural logarithm of 2, used in the computation of the number of bits. */
    private final static double NATURAL_LOG_OF_2 = Math.log(2);

    private int size = 0;

    public DistributedSign() {
        int len = (int) Math.ceil(((long) maxNumber * (long) hashFunction / NATURAL_LOG_OF_2) / 32);
        this.totalBits = len * 32L;
        bits = new int[len];
        Random random = new Random(seed);
        weight = new int[hashFunction][];
        for (int i = 0; i < hashFunction; i++) {
            weight[i] = new int[NUMBER_OF_WEIGHTS];
            for (int j = 0; j < NUMBER_OF_WEIGHTS; j++)
                weight[i][j] = random.nextInt();
        }
    }

    public static DistributedSign unSign(String sign) {
        byte[] decode = BaseSixtyfour.decode(sign);
        DistributedSign distributedSign = new DistributedSign();

        distributedSign.size = ((int) decode[0] << 24) | (int) decode[1] << 18 | (int) decode[2] << 8 | (int) decode[3];
        int n = decode.length / 4 - 1;
        int[] bits = new int[n];
        for (int i = 0; i < n; i++) {
            bits[i] = (int) decode[i * 4 + 4] << 24 | (int) decode[i * 4 + 5] << 16 | (int) decode[i * 4 + 6] << 8
                    | decode[i * 4 + 7];
        }
        distributedSign.bits = bits;
        return distributedSign;
    }

    public String sign() {
        byte[] data = new byte[bits.length * 4 + 4];
        data[0] = (byte) ((size >>> 24) & 0xff);
        data[1] = (byte) ((size >>> 16) & 0xff);
        data[2] = (byte) ((size >>> 8) & 0xff);
        data[3] = (byte) ((size) & 0xff);
        for (int i = 0; i < bits.length; i++) {
            data[i * 4 + 4] = (byte) ((bits[i] >>> 24) & 0xff);
            data[i * 4 + 5] = (byte) ((bits[i] >>> 16) & 0xff);
            data[i * 4 + 6] = (byte) ((bits[i] >>> 8) & 0xff);
            data[i * 4 + 7] = (byte) ((bits[i]) & 0xff);
        }
        return BaseSixtyfour.encode(data);
    }

    /**
     * 使用指定的hash参数产生hash值。对于同样的字符串,在不同hash参数下应该产生不同的hash value
     *
     * @param s a character sequence.
     * @param strLength the length of <code>s</code>.
     * @param funtionIndex a hash function index (smaller than {@link #totalBits}).
     * @return the position in the filter corresponding to <code>s</code> for the hash function <code>k</code>.
     */
    private long hash(final CharSequence s, final int strLength, final int funtionIndex) {
        final int[] w = weight[funtionIndex];
        int h = 0, i = strLength;
        while (i-- != 0)
            h ^= s.charAt(i) * w[i % NUMBER_OF_WEIGHTS];
        return ((long) h - Integer.MIN_VALUE) % totalBits;
    }

    /**
     * Checks whether the given character sequence is in this filter.
     *
     * <P>
     * Note that this method may return true on a character sequence that is has not been added to the filter. This will
     * happen with probability 2<sub>-<var>d</var></sub>, where <var>d</var> is the number of hash functions specified
     * at creation time, if the number of the elements in the filter is less than <var>n</var>, the number of expected
     * elements specified at creation time.
     *
     * @param str a character sequence.
     * @return true if the sequence is in the filter (or if a sequence with the same hash sequence is in the filter).
     */

    public boolean contains(final CharSequence str) {
        int i = hashFunction, length = str.length();
        while (i-- != 0)
            if (!getBit(hash(str, length, i)))
                return false;
        return true;
    }

    /**
     * Adds a character sequence to the filter.
     *
     * @param s a character sequence.
     * @return true if the character sequence was not in the filter (but see {@link #contains(CharSequence)}).
     */

    public boolean add(final CharSequence s) {
        boolean result = false;
        int i = hashFunction, length = s.length();
        long hash;
        while (i-- != 0) {
            hash = hash(s, length, i);
            if (!getBit(hash)) {
                result = true;
            }
            setBit(hash);
        }
        if (result)
            size++;
        return result;
    }

    protected final static long ADDRESS_BITS_PER_UNIT = 5; // 32=2^5
    protected final static long BIT_INDEX_MASK = 31; // = BITS_PER_UNIT - 1;

    /**
     * Returns from the local bitvector the value of the bit with the specified index. The value is <tt>true</tt> if the
     * bit with the index <tt>bitIndex</tt> is currently set; otherwise, returns <tt>false</tt>.
     *
     * (adapted from cern.colt.bitvector.QuickBitVector)
     *
     * @param bitIndex the bit index.
     * @return the value of the bit with the specified index.
     */
    protected boolean getBit(long bitIndex) {
        return ((bits[(int) (bitIndex >> ADDRESS_BITS_PER_UNIT)] & (1 << (bitIndex & BIT_INDEX_MASK))) != 0);
    }

    /**
     * Changes the bit with index <tt>bitIndex</tt> in local bitvector.
     *
     * (adapted from cern.colt.bitvector.QuickBitVector)
     *
     * @param bitIndex the index of the bit to be set.
     */
    protected void setBit(long bitIndex) {
        bits[(int) (bitIndex >> ADDRESS_BITS_PER_UNIT)] |= 1 << (bitIndex & BIT_INDEX_MASK);
    }

    /*
     * (non-Javadoc)
     * @see org.archive.util.BloomFilter#getSizeBytes()
     */
    public long getSizeBytes() {
        return bits.length * 4;
    }

    public boolean contains(Proxy proxy) {
        return contains(proxy.getIp() + ":" + proxy.getPort());
    }

    public boolean contains(ProxyModel proxy) {
        return contains(proxy.getIp() + ":" + proxy.getPort());
    }

    public static String resign(String usedSign, List<ProxyModel> distribute) {
        DistributedSign sign = null;
        if (!StringUtils.isEmpty(usedSign)) {
            try {
                sign = DistributedSign.unSign(usedSign);
            } catch (Exception e) {
            }
        }
        if (sign == null) {
            sign = new DistributedSign();
        }
        int failedNumber = 0;
        for (ProxyModel proxyModel : distribute) {
            if (!sign.add(proxyModel.getIp() + ":" + proxyModel.getPort())) {
                failedNumber++;
            }
        }
        if (failedNumber > distribute.size() / 5) {
            return empty;
        }
        return sign.sign();
    }

    private static String empty = new DistributedSign().sign();
}
