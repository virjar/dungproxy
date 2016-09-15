DROP DATABASE IF EXISTS proxyipcenter;
CREATE DATABASE proxyipcenter;
USE proxyipcenter;
CREATE TABLE proxy (
  id                    BIGINT PRIMARY KEY                                                                               AUTO_INCREMENT
  COMMENT '主键',
  ip                    CHAR(20)  NOT NULL
  COMMENT 'IP地址',
  proxy_ip              CHAR(20) COMMENT '代理IP，也就是目标网站最终看到的IP（多级代理的情况ip和proxy_ip不会相同）',
  port                  INT(5) COMMENT '端口号 可能有5位的可能性',
  ip_value              BIGINT COMMENT 'ip的数字表示，用于过滤连续IP问题',
  country               VARCHAR(255) COMMENT '国家',
  area                  VARCHAR(255) COMMENT '地区',
  region                VARCHAR(255) COMMENT '省',
  city                  VARCHAR(255) COMMENT '市',
  isp                   VARCHAR(255) COMMENT '运营商',
  country_id            VARCHAR(255) COMMENT '国家代码',
  area_id               VARCHAR(255) COMMENT '地区代码',
  region_id             VARCHAR(255) COMMENT '省级代码',
  city_id               VARCHAR(255) COMMENT '城市代码',
  isp_id                VARCHAR(255) COMMENT 'isp代码',
  address_id            BIGINT COMMENT '地理位置ID，融合各个地理位置获取的一个数字，数值约接近表示实际地理位置约接近',
  transperent           TINYINT COMMENT '透明度(高匿，普通，透明)',
  speed                 BIGINT COMMENT '连接时间（越小速度越快）',
  type                  TINYINT COMMENT '类型（http，https,httpAndHttps,socket,qq）',
  connection_score      BIGINT   NOT NULL                                                                                DEFAULT 0
  COMMENT '连接性打分',
  availbel_score        BIGINT   NOT NULL                                                                                DEFAULT 0
  COMMENT '可用性打分',
  connection_score_date DATETIME COMMENT '连接性打分时间',
  availbel_score_date   DATETIME COMMENT '可用性打分时间',
  createtime            TIMESTAMP NOT NULL                                                                               DEFAULT CURRENT_TIMESTAMP
  COMMENT '收录时间',
  support_gfw           TINYINT(1)                                                                                       DEFAULT NULL
  COMMENT '是否支持翻墙',
  gfw_speed             BIGINT COMMENT '翻墙访问速度',
  source                VARCHAR(255) COMMENT '资源来源url',
  crawler_key           VARCHAR(255) COMMENT '爬虫key，用户统计爬虫收集分布',
  lostheader            BOOLEAN COMMENT '是否会丢失http头部'
)
  ENGINE = innoDB, CHARSET = utf8;

/*用于反馈子系统*/
CREATE TABLE domainqueue (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT
  COMMENT '主键',
  domain VARCHAR(255) NOT NULL
  COMMENT '域名',
  proxy_id BIGINT COMMENT '代理IP的ID',
  ip       CHAR(20)   NOT NULL
  COMMENT 'IP地址',
  port     INT(4) COMMENT '端口号',
  domain_score BIGINT             DEFAULT 0
  COMMENT '域名下打分'
)
  ENGINE = innoDB, CHARSET = utf8;

