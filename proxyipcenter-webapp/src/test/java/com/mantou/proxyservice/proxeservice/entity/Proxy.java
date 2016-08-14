package com.mantou.proxyservice.proxeservice.entity;

import java.util.Date;

public class Proxy {
    private Integer id;

    private String ip;

    private Integer port;

    private String country;

    private String address;

    private Integer transperent;

    private Integer type;

    private Integer availblelevel;

    private Date lastupdate;

    private Integer speed;

    private Integer stability;

    private Integer testtimes;
    
    private Integer connectionlevel;
    
    private Integer direction;
    
    private Integer googlesupport;
    
    private String source;

    private Date lastavaibleupdate;
    
    public Date getLastavaibleupdate() {
		return lastavaibleupdate;
	}

	public void setLastavaibleupdate(Date lastavaibleupdate) {
		this.lastavaibleupdate = lastavaibleupdate;
	}

	public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getTransperent() {
        return transperent;
    }

    public void setTransperent(Integer transperent) {
        this.transperent = transperent;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    
    public Date getLastupdate() {
        return lastupdate;
    }

    public void setLastupdate(Date lastupdate) {
        this.lastupdate = lastupdate;
    }

    public Integer getAvailblelevel() {
		return availblelevel;
	}

	public void setAvailblelevel(Integer availblelevel) {
		this.availblelevel = availblelevel;
	}

	public Integer getConnectionlevel() {
		return connectionlevel;
	}

	public void setConnectionlevel(Integer connectionlevel) {
		this.connectionlevel = connectionlevel;
	}

	public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Integer getStability() {
        return stability;
    }

    public void setStability(Integer stability) {
        this.stability = stability;
    }

    public Integer getTesttimes() {
        return testtimes;
    }

    public void setTesttimes(Integer testtimes) {
        this.testtimes = testtimes;
    }

	
	public Integer getDirection() {
		return direction;
	}

	public void setDirection(Integer direction) {
		this.direction = direction;
	}

	public Integer getGooglesupport() {
		return googlesupport;
	}

	public void setGooglesupport(Integer googlesupport) {
		this.googlesupport = googlesupport;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

    @Override
    public String toString() {
        return "Proxy{" +
                "port=" + port +
                ", ip='" + ip + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}