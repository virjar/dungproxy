package com.virjar.dungproxy.client.model;

import com.google.common.base.Objects;
import com.virjar.dungproxy.client.ippool.config.DomainContext;

/**
 * Created by virjar on 16/10/29.<br/>
 * 他是云代理,这种类型的代理永远不下线,但是也需要参与IP使用的竞争
 */
public class CloudProxy extends AvProxy {
    /**
     * 云代理可以有副本数目
     */
    private int offset = 0;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public CloudProxy(DomainContext domainContext) {
        super(domainContext);
    }

    /**
     * 统一代理服务不走普通下线策略逻辑,因为她的失败不能表示统一代理服务器不可用。而可能是统一代理服务的上游挂了 另外,统一代理服务目前没有自动上线逻辑
     */
    @Override
    public void offline() {
        // 不下线
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CloudProxy other = (CloudProxy) o;
        return Objects.equal(this.offset, other.offset) && Objects.equal(this.getUsername(), other.getUsername())
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(offset, getUsername(), getIp(), getPort());
    }
}
