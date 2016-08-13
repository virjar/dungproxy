drop database if exists proxyipcenter;
create database proxyipcenter;
use proxyipcenter;
create table proxy(
	id bigint primary key auto_increment comment '主键',
    ip char(20) not null comment 'IP地址',
    proxy_ip char(20) comment '代理IP，也就是目标网站最终看到的IP（多级代理的情况ip和proxy_ip不会相同）',
    port int(5) comment '端口号 可能有5位的可能性',
    ip_value bigint comment 'ip的数字表示，用于过滤连续IP问题',
    country varchar(255) comment '国家',
    area varchar(255) comment '地区',
    region varchar(255) comment '省',
    city varchar(255) comment '市',
    isp varchar(20) comment '运营商',
    country_id int comment '国家代码',
    area_id int comment '地区代码',
    region_id int comment '省级代码',
    city_id int comment '城市代码',
    isp_id int comment 'isp代码',
    address_id bigint comment '地理位置ID，融合各个地理位置获取的一个数字，数值约接近表示实际地理位置约接近',
    transperent tinyint comment '透明度(高匿，普通，透明)',
    speed bigint comment '连接时间（越小速度越快）',
    type  tinyint comment '类型（http，https,httpAndHttps,socket,qq）',
    connection_score bigint not null default 0 comment '连接性打分',
    availbel_score bigint not null default 0 comment '可用性打分',
    connection_score_date date comment '连接性打分时间',
    availbel_score_date date comment '可用性打分时间',
    createtime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '收录时间',
    support_gfw tinyint(1) default null comment '是否支持翻墙',
    gfw_speed bigint comment '翻墙访问速度',
    source varchar(255) comment '资源来源url',
    crawler_key varchar(255) comment '爬虫key，用户统计爬虫收集分布'
) engine=innoDB,charset=utf8;

/*用于反馈子系统*/
create table domainqueue(
	id bigint primary key auto_increment comment '主键',
    domain varchar(255) not null comment '域名',
    proxy_id bigint comment '代理IP的ID',
    ip char(20) not null comment 'IP地址',
    port int(4) comment '端口号',
    domain_score bigint default 0 comment '域名下打分'
)engine=innoDB,charset=utf8;

