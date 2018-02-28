
### 和webCollector的集成

在0.0.8之后,取消对webCollector的支持,实践来看webCollector的设计并不符合我的口味,虽然我之前用过webCollector😓

如果你仍然需要使用WebCollector,可以复制demo项目源码,作为桥接。dungproxy本身不在core内部支持WebCollector。

dungproxy不直接支持WebCollector的另一个原因是WebCollector API发生了不兼容改动,导致用户在使用dungproxy-core内部支持的WebCollector桥接层时报告异常

兼容层demo地址:

1. webcollector 2.32版本 [dungproxy-webcollector-2_32](https://gitee.com/virjar/proxyipcenter/dungproxy-webcollector-2_32)
2. webcollector 2.71版本 [dungproxy-webcollector-2_32](https://gitee.com/virjar/proxyipcenter/dungproxy-webcollector-2_71)

如果有用户需要,我可以继续维护对多个WebCollector桥接分支。