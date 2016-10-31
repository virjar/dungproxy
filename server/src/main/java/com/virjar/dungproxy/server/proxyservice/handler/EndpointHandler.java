package com.virjar.dungproxy.server.proxyservice.handler;

import com.google.common.base.Optional;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Description: 处理Client端Exception, 并释放资源.
 *
 * @author lingtong.fu
 * @version 2016-10-18 16:19
 */
public abstract class EndpointHandler extends ChannelDuplexHandler {

    protected static Optional<String> getParam(Map<String, List<String>> paramMap, String key) {
        List<String> params = paramMap.get(key);
        if (CollectionUtils.isEmpty(params)) {
            return Optional.absent();
        }
        return Optional.of(params.get(0));
    }

    protected Map<String, List<String>> getRequestParam(String uri) {
        //默认使用UTF_8解码
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        return decoder.parameters();
    }
}
