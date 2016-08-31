package com.virjar.distributer;

import java.util.Random;

/**
 * 标示已经被分配过的IP,使用bloomFilter的思想,传递一个签名 Created by virjar on 16/8/31. 该容器
 */
public class DistributedSign {
    // 每个签名容器最多存500个IP使用信息,如果超过,服务器将会强制重置
    private static final int maxNumber = 500;
    // hash标记位,一般来说这个数值越高越不容易出现数据冲突,但是相对来说容器大小将会增加。服务器使用的是22个,考虑客户端数据量较小,设置为10
    private static final int hashFunction = 10;
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

    public String sign() {
        return null;
    }

    public void add(String str) {

    }
}
