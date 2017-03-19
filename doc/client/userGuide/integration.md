# 在java工程里面使用dungclient
##在普通java工程里面引入dungclient
dungclient所有jar包在client/dungclient/lib,拷贝所有jar包到你的lib下,并将其添加到classpath即可
##通过maven的方式引入dungclient
在``pom.xml``中添加项目依赖
```
<dependency>
    <groupId>com.virjar</groupId>
    <artifactId>dungproxy-client</artifactId>
    <version>0.0.5</version>
</dependency>
```
 