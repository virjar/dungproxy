package com.virjar.dungproxy.client;

import java.io.Serializable;

/**
 * Created by wenxin.fan on 2015/5/21.
 */
public class Phone implements Serializable {

    private static final long serialVersionUID = -6732131032651082674L;

    private String province;
    private String city;
    private short mmo;

    public Phone() {
    }

    public Phone(String province, String city, short mmo) {
        this.province = province;
        this.city = city;
        this.mmo = mmo;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public short getMmo() {
        return mmo;
    }

    public void setMmo(short mmo) {
        this.mmo = mmo;
    }

    @Override
    public String toString() {
        return "Phone [" +
                "province=" + province +
                ", city=" + city +
                ", mmo=" + mmo +
                "]";
    }
}
