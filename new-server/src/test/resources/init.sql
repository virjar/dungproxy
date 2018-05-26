DROP DATABASE IF EXISTS dungproxy;
CREATE DATABASE dungproxy;
USE dungproxy;

CREATE TABLE `proxy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ip_value` bigint(20) DEFAULT NULL COMMENT 'ip的数字表示，四个字节ip位,两个字节port位',
  `ip` char(20) NOT NULL COMMENT 'IP地址',
  `port` int(5) DEFAULT NULL COMMENT '端口号',
  `proxy_ip` char(20) DEFAULT NULL COMMENT '代理IP，也就是目标网站最终看到的IP（多级代理的情况ip和proxy_ip不会相同）',
  `transparent` char(10) DEFAULT 'transparent' COMMENT '透明度(高匿，普通，透明)',
  `type` char(10) DEFAULT 'http' COMMENT '类型（http，https,httpAndHttps,socket,qq',
  `score` int(5) DEFAULT 0 COMMENT '该ip可用分数',
  `state` tinyint(2) DEFAULT 0 COMMENT '该ip状态,0初始化,校验中1,坏资源2',
  `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收录时间',
  `lastUpdate` timestamp NOT NULL   COMMENT '本记录最后更新时间',
  `country` char(40)   DEFAULT COMMENT '该ip所在国家',
  PRIMARY KEY (`id`),
  UNIQUE  `uniqe_ip_value` (`ip_value`),
  INDEX `index_state` (`state`),
  INDEX `index_score` (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;