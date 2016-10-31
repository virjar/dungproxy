package com.virjar.dungproxy.server.distributer;

/**
 * ip资源请求封装 Created by virjar on 16/9/2.
 */
public class RequestForm {
    private String usedSign;// 资源签名,标记已经分配过的资源
    private Integer num; // 期望获取资源数目
    private Integer transparent; // 期望透明度
    private Boolean headerLoseAllow = true; // 是否代理IP转发请求的时候丢弃http头部(普通网页网站应该是允许的,API服务应该不允许)
    private Boolean matchISP = false;// 期望ISP和目标网站匹配
    private Boolean matchAddress = false; // 期望地址和目标网站接近
    private Boolean userInteral = false;// 期望是国外的地址(一般支持翻墙)
    private Boolean supportHttps = false;// 需要支持https
    private Integer maxPing = 10000;// 最大ping值
    private String distributeStrategy = "soft";// soft strict
    private String country;// 匹配国家 如中国
    private String area;// 匹配地区 如:华北,西南
    private String isp;// 匹配ISP 如:移动
    private String matchSequnce;// 资源数量压缩的时候对复合规则进行排序的顺序,在soft和mixed模式下,如果资源不够,采取备用数据的排序方式
    // 资源数量压缩的时候对复合规则进行排序的顺序,在soft和mixed模式下,如果资源不够,采取备用数据的排序方式,
    // 和matchSequnce不同的是,他不会根据标量按照顺序过滤,而是根据运算规则指定权重排序
    private String matchExpression;// transparent:10,matchISP:100,maxPing:50 -1/maxPing
    private String domain;// 打算访问的目标网站域名
    private String checkUrl;
    private Boolean supportPost;// 很多代理可能只能支持GET方法,不支持POST这个字段先留着,暂不支持通过这种方式过滤

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDistributeStrategy() {
        return distributeStrategy;
    }

    public void setDistributeStrategy(String distributeStrategy) {
        this.distributeStrategy = distributeStrategy;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getHeaderLoseAllow() {
        return headerLoseAllow;
    }

    public void setHeaderLoseAllow(Boolean headerLoseAllow) {
        this.headerLoseAllow = headerLoseAllow;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public Boolean getMatchAddress() {
        return matchAddress;
    }

    public void setMatchAddress(Boolean matchAddress) {
        this.matchAddress = matchAddress;
    }

    public String getMatchExpression() {
        return matchExpression;
    }

    public void setMatchExpression(String matchExpression) {
        this.matchExpression = matchExpression;
    }

    public Boolean getMatchISP() {
        return matchISP;
    }

    public void setMatchISP(Boolean matchISP) {
        this.matchISP = matchISP;
    }

    public String getMatchSequnce() {
        return matchSequnce;
    }

    public void setMatchSequnce(String matchSequnce) {
        this.matchSequnce = matchSequnce;
    }

    public Integer getMaxPing() {
        return maxPing;
    }

    public void setMaxPing(Integer maxPing) {
        this.maxPing = maxPing;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Boolean getSupportHttps() {
        return supportHttps;
    }

    public void setSupportHttps(Boolean supportHttps) {
        this.supportHttps = supportHttps;
    }

    public Integer getTransparent() {
        return transparent;
    }

    public void setTransparent(Integer transparent) {
        this.transparent = transparent;
    }

    public String getUsedSign() {
        return usedSign;
    }

    public void setUsedSign(String usedSign) {
        this.usedSign = usedSign;
    }

    public Boolean getUserInteral() {
        return userInteral;
    }

    public void setUserInteral(Boolean userInteral) {
        this.userInteral = userInteral;
    }

    public String getCheckUrl() {
        return checkUrl;
    }

    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }
}
