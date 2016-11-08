package com.virjar.dungproxy.server.proxyservice.common.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCounted;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Description: NetworkUtil
 *
 * @author lingtong.fu
 * @version 2016-10-18 16:53
 */
public class NetworkUtil {

    private static final Set<String> topLevelDomain = Sets.newHashSet(
            "ac", "ad", "ae", "aero", "af", "ag", "ai", "al", "am", "an", "ao", "aq", "ar", "as", "at",
            "au", "aw", "az", "ba", "bb", "bd", "be", "bf", "bg", "bh", "bi", "biz", "bj", "bm", "bn",
            "bo", "br", "bs", "bt", "bv", "bw", "by", "bz", "ca", "cc", "cd", "cf", "cg", "ch", "ci",
            "ck", "cl", "cm", "cn", "co", "com", "coop", "cr", "cu", "cv", "cx", "cy", "cz", "de", "dj",
            "dk", "dm", "do", "dz", "ec", "edu", "ee", "eg", "eh", "er", "es", "et", "eu", "fi", "fj",
            "fk", "fm", "fo", "fr", "ga", "gd", "ge", "gf", "gg", "gh", "gi", "gl", "gm", "gn", "gov",
            "gp", "gq", "gr", "gs", "gt", "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id",
            "idv", "ie", "il", "im", "in", "info", "int", "io", "iq", "ir", "is", "it", "je", "jm", "jo",
            "jp", "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li",
            "lk", "lr", "ls", "lt", "lu", "lv", "ly", "ma", "mc", "md", "mg", "mh", "mil", "mk", "ml", "mm",
            "mn", "mo", "mp", "mq", "mr", "ms", "mt", "mu", "museum", "mv", "mw", "mx", "my", "mz", "na", "name",
            "nc", "ne", "net", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om", "org", "pa", "pe",
            "pf", "pg", "ph", "pk", "pl", "pm", "pn", "pr", "pro", "ps", "pt", "pw", "py", "qa", "re", "ro",
            "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sj", "sk", "sl", "sm", "sn", "so",
            "sr", "st", "sv", "sy", "sz", "tc", "td", "tf", "tg", "th", "tj", "tk", "tl", "tm", "tn", "to",
            "tp", "tr", "tt", "tv", "tw", "tz", "ua", "ug", "uk", "um", "us", "uy", "uz", "va", "vc", "ve",
            "vg", "vi", "vn", "vu", "wf", "ws", "xxx", "ye", "yr", "yt", "yu", "za", "zm", "zw");

    public static void closeChannel(Channel... channels) {
        for (Channel ch : channels) {
            if (ch != null) {
                ch.close();
            }
        }
    }

    /**
     * 检查端口是否被占用
     */
    public static boolean isPortAvailable(int port) {
        if (checkPort(port)) {
            try {
                bindPort("0.0.0.0", port);
                bindPort(InetAddress.getLocalHost().getHostAddress(), port);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private static boolean checkPort(int port) {
        return (port > 0 && port < 65535);
    }

    private static void bindPort(String host, int port) throws Exception {
        Socket s = new Socket();
        s.bind(new InetSocketAddress(host, port));
        s.close();
    }

    public static void writeAndFlushAndClose(Channel channel, FullHttpResponse response) {
        NetworkUtil.resetHandler(channel.pipeline(), new HttpResponseEncoder());
        channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void resetHandler(ChannelPipeline pipeline, ChannelHandler handler) {
        ChannelHandler handler1;
        if ((handler1 = pipeline.get(handler.getClass())) != null) {
            pipeline.remove(handler1);
        }
        pipeline.addLast(handler);
    }

    public static void releaseMsgCompletely(Object msg) {
        if (msg != null && msg instanceof ReferenceCounted) {
            ReferenceCounted rc = (ReferenceCounted) msg;
            if (rc.refCnt() > 0) {
                rc.release(rc.refCnt());
            }
        }
    }

    public static void releaseMsg(Object msg) {
        if (msg != null && msg instanceof ReferenceCounted) {
            ReferenceCounted rc = (ReferenceCounted) msg;
            if (rc.refCnt() > 0) {
                rc.release();
            }
        }
    }

    public static void removeHandler(ChannelPipeline p, Class<? extends ChannelHandler> clazz) {
        if (p.get(clazz) != null) {
            p.remove(clazz);
        }
    }

    public static void addHandlerIfAbsent(ChannelPipeline pipeline, ChannelHandler handler) {
        if (pipeline.get(handler.getClass()) == null) {
            pipeline.addLast(handler);
        }
    }

    public static String requestToCurl(HttpRequest request) {
        String uri = request.uri();
        HttpHeaders headers = request.headers();
        StringBuilder sb = new StringBuilder("");
        sb.append("curl -v '");
        sb.append(uri);
        sb.append("' ");
        for (Map.Entry header : headers.entries()) {
            sb.append("-H '");
            sb.append(header.getKey());
            sb.append(":");
            sb.append(header.getValue());
            sb.append("' ");
        }
        return sb.toString();
    }

    public static <T> void setAttr(Channel channel, AttributeKey<T> key, T t) {
        channel.attr(key).set(t);
    }

    public static String getIp(Channel ch) {
        return ((InetSocketAddress) ch.remoteAddress()).getAddress().getHostAddress();
    }

    public static boolean isSchemaHttps(String url) {
        return url.length() >= 5 && url.substring(0, 5).toLowerCase().equals("https");
    }

    public static boolean isCodeValid(int code) {
        return code > 0 && code < 400;
    }

    public static int getPortFromHostHeader(String host) {
        int splitPos = host.indexOf(":");
        if (splitPos < 0) return -1;
        int port;
        try {
            port = Integer.valueOf(host.substring(splitPos + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
        return port;
    }

    public static void removeAllFromPipelineExcept(ChannelPipeline pipeline, List<Class> clazzList) {
        List<String> namesToRemove = Lists.newArrayList();
        for (Map.Entry<String, ChannelHandler> handlerEntry : pipeline) {
            Class clazz = handlerEntry.getValue().getClass();
            if (clazzList.contains(clazz)) continue;
            namesToRemove.add(handlerEntry.getKey());
        }
        for (String toRemove : namesToRemove) {
            pipeline.remove(toRemove);
        }
    }

    public static <T> T getAttr(Channel channel, AttributeKey<T> key) {
        T t = channel.attr(key).get();
        if (t == null) {
            throw new NullPointerException(key + " 不存在");
        }
        return t;
    }

    /**
     * 获取主域名
     *
     * @return 主域名
     */
    public static String getMainHost(String host) {
        List<String> ss = Lists.newArrayList(Splitter.on(".").split(host));
        StringBuilder domain = new StringBuilder("");
        int topLevelDomainIdx = -1;
        for (int i = ss.size() - 1; i >= 0; i--) {
            String s = ss.get(i);
            if (topLevelDomain.contains(s)) {
                domain.insert(0, s);
                domain.insert(0, ".");
                topLevelDomainIdx = i;
            } else {
                break;
            }
        }
        if (topLevelDomainIdx == -1) {
            return host;
        } else {
            if (topLevelDomainIdx == 0) {
                return host;
            } else {
                domain.insert(0, ss.get(topLevelDomainIdx - 1));
                return domain.toString();
            }
        }
    }
}
