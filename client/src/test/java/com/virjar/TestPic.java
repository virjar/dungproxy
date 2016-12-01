package com.virjar;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.common.collect.Maps;

/**
 * Created by virjar on 16/12/1.
 */
public class TestPic {
    private static final Map<Integer,Integer> signMap = Maps.newHashMap();
    public static void main(String[] args) throws IOException {
        signMap.put(-2022082277,3);
        signMap.put(114282345,6);
        signMap.put(973144210,8);
        signMap.put(-2107415806,5);
        signMap.put(-2040043769,2);
        signMap.put(1734632467,7);
        signMap.put(-252974184,1);
        signMap.put(319622317,4);
        signMap.put(-694576847,0);
        signMap.put(182180725,9);
        BufferedImage pic = ImageIO.read(new File("/Users/virjar/Desktop/work/cztouch/image.png"));
        BufferedImage pic1 = ImageIO.read(new File("/Users/virjar/Desktop/work/cztouch/image1.png"));
        for(int i=0;i<10;i++){
            System.out.println(signMap.get(sign(i,pic)));
        }
        System.out.println("------");
        for(int i=0;i<10;i++){
            System.out.println(signMap.get(sign(i,pic1)));
        }

    }

    private static int sign(int offset, BufferedImage pic) {
        int ret = 0;
        int x = offset * 104;
        int height = pic.getHeight();
        for (int i = 0; i < 104; i++) {
            for (int j = 0; j < height; j++) {
                ret += pic.getRGB(x + i, j);
            }
        }
        return ret;
    }
}
