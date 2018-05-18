package com.virjar.dungproxy.newserver.model;

import lombok.Data;

import java.util.Date;

/**
 * Created by virjar on 2018/5/18.<br>
 */
@Data
public class ProxyModel {
    //主键
    private Long id;
    //ip的int值,四个字节可以描述,为了避免溢出,使用long
    private long ipInt;
    //ip的字符串值
    private String ip;
    //端口
    private int port;
    //透明度
    private String transparent;
    //类型,为http/https还是socket代理
    private String type;
    //本资源的分数,可以为负数
    private int score;
    //本记录创建时间
    private Date createDate;
    //本记录最后更新时间
    private Date lastUpdate;
    //本ip所在国家,现在不区分具体省市县,实际发现无意义
    private String country;
}
