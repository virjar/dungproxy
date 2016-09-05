package com.virjar.model;

public class Proxy {
    private long id;
    private String host;
    private int port;
    private boolean available;
    private int https;
    private String auth;
    private Tag tag;
    private String country;
    private String province;
    private String city;
    private Type type;
    private int userAgentHash;
    private int trafficLimit;

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setHttps(int https) {
        this.https = https;
    }

    public long getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isFree() {
        return tag == Tag.FREE;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isSocks() {
        return type == Type.SOCKSV5_NOAUTH;
    }

    public Tag getTag() {
        return tag;
    }

    public int getHttps() {
        return https;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public String getAuth() {
        return auth;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public int getUserAgentHash() {
        return userAgentHash;
    }

    public void setUserAgentHash(int userAgentHash) {
        this.userAgentHash = userAgentHash;
    }

    public int getTrafficLimitPerSecond() {
        return trafficLimit;
    }

    public void setTrafficLimit(int trafficLimit) {
        this.trafficLimit = trafficLimit;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Proxy proxy = (Proxy) o;

        if (port != proxy.port)
            return false;
        if (!host.equals(proxy.host))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "Proxy{" +
                "id=" + id +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", available=" + available +
                ", https=" + https +
                ", auth='" + auth + '\'' +
                ", tag=" + tag +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", userAgentHash=" + userAgentHash +
                '}';
    }

    public enum Type implements Status {
        HTTP(1, "HTTP 代理"), SOCKSV5_NOAUTH(2, "SOCKS5 无验证代理");

        private final int code;
        private final String desc;

        Type(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public static Proxy.Type codeOf(int code) {
            for (Type type : Type.values()) {
                if (type.code == code) return type;
            }
            throw new IllegalArgumentException("code " + code + " Type类型代理不存在");
        }
        public int code() {
            return code;
        }

        public String desc() {
            return desc;
        }
    }

    public enum Tag implements Status {

        UNKNOWN(0, "未知"),
        ADSL(1, "ADSL代理"),
        FREE(3, "免费代理"),
        SOCKS5_NOAUTH(99, "socks5 no auth"),
        FIXED_IP_FLI(4, "固定代理");

        public final int code;
        public final String desc;

        Tag(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public int code() {
            return code;
        }

        public String desc() {
            return desc;
        }

        public static Proxy.Tag codeOf(int code) {
            for (Proxy.Tag tag : Tag.values()) {
                if (tag.code == code) return tag;
            }
            throw new IllegalArgumentException("code " + code + " 类型代理不存在");
        }
    }
}
