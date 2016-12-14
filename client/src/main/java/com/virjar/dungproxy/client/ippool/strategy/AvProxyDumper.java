package com.virjar.dungproxy.client.ippool.strategy;

import java.util.List;
import java.util.Map;

import com.virjar.dungproxy.client.model.AvProxy;

/**
 * 序列化本地IP池,程序启动的时候将会尝试通过他来还原IP池<br/>
 * Created by virjar on 16/10/4.
 */
public interface AvProxyDumper {
	
	
	void setDumpFileName(String dumpFileName);
    /**
     * 序列化,传入为multiMap结构,根据domain区分,每个domain下面是可用代理IP列表
     */
    void serializeProxy(Map<String, List<AvProxy>> data);

    /**
     * 反序列化
     * 
     * @return 反序列化后的multiMap格式数据
     */
    Map<String, List<AvProxy>> unSerializeProxy();

    /**
     * 设置序列化的文件路径,文件可以是虚拟的,或者网上,只要实现类可以识别他
     * @param path 路径信息
     */
    void setPath(String path);
}
