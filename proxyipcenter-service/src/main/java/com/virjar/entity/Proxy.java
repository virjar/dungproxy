package com.virjar.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Proxy {
    private Long id;

    private String ip;

    private String proxyIp;

    private Integer port;

    private Long ipValue;

    private String country;

    private String area;

    private String region;

    private String city;

    private String isp;

    private Integer countryId;

    private Integer areaId;

    private Integer regionId;

    private Integer cityId;

    private Integer ispId;

    private Long addressId;

    private Byte transperent;

    private Long speed;

    private Byte type;

    private Long connectionScore;

    private Long availbelScore;

    private Date connectionScoreDate;

    private Date availbelScoreDate;

    private Date createtime;

    private Boolean supportGfw;

    private Long gfwSpeed;

    private String source;

    private String crawlerKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getProxyIp() {
        return proxyIp;
    }

    public void setProxyIp(String proxyIp) {
        this.proxyIp = proxyIp;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Long getIpValue() {
        return ipValue;
    }

    public void setIpValue(Long ipValue) {
        this.ipValue = ipValue;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }

    public Integer getAreaId() {
        return areaId;
    }

    public void setAreaId(Integer areaId) {
        this.areaId = areaId;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public Integer getIspId() {
        return ispId;
    }

    public void setIspId(Integer ispId) {
        this.ispId = ispId;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public Byte getTransperent() {
        return transperent;
    }

    public void setTransperent(Byte transperent) {
        this.transperent = transperent;
    }

    public Long getSpeed() {
        return speed;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public Byte getType() {
        return type;
    }

    public void setType(Byte type) {
        this.type = type;
    }

    public Long getConnectionScore() {
        return connectionScore;
    }

    public void setConnectionScore(Long connectionScore) {
        this.connectionScore = connectionScore;
    }

    public Long getAvailbelScore() {
        return availbelScore;
    }

    public void setAvailbelScore(Long availbelScore) {
        this.availbelScore = availbelScore;
    }

    public Date getConnectionScoreDate() {
        return connectionScoreDate;
    }

    public void setConnectionScoreDate(Date connectionScoreDate) {
        this.connectionScoreDate = connectionScoreDate;
    }

    public Date getAvailbelScoreDate() {
        return availbelScoreDate;
    }

    public void setAvailbelScoreDate(Date availbelScoreDate) {
        this.availbelScoreDate = availbelScoreDate;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Boolean getSupportGfw() {
        return supportGfw;
    }

    public void setSupportGfw(Boolean supportGfw) {
        this.supportGfw = supportGfw;
    }

    public Long getGfwSpeed() {
        return gfwSpeed;
    }

    public void setGfwSpeed(Long gfwSpeed) {
        this.gfwSpeed = gfwSpeed;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCrawlerKey() {
        return crawlerKey;
    }

    public void setCrawlerKey(String crawlerKey) {
        this.crawlerKey = crawlerKey;
    }
}