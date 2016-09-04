package com.virjar.model;

import java.io.Serializable;

/**
 * Description: Phone
 *
 * @author lingtong.fu
 * @version 2016-09-05 00:01
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
