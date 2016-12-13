package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.virjar.dungproxy.client.ippool.strategy.AvProxyDumper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.virjar.dungproxy.client.ippool.config.ProxyConstant;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Created by virjar on 16/10/4.
 */
public class PlainTextFileAvProxyDumper implements AvProxyDumper {
	private Logger logger = LoggerFactory.getLogger(PlainTextFileAvProxyDumper.class);
	private String dumpFileName;

	public PlainTextFileAvProxyDumper() {
		// TODO Auto-generated constructor stub
	}
	

	@Override
	public void setDumpFileName(String dumpFileName) {
		// TODO Auto-generated method stub
		this.dumpFileName = dumpFileName;
	}
	
	@Override
	public void serializeProxy(Map<String, List<AvProxy>> data) {
		data = Maps.transformValues(data, new Function<List<AvProxy>, List<AvProxy>>() {
			@Override
			public List<AvProxy> apply(List<AvProxy> input) {
				return Lists.transform(input, new Function<AvProxy, AvProxy>() {
					@Override
					public AvProxy apply(AvProxy input) {
						input.setDomainPool(null);
						return input;
					}
				});
			}
		});

		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = Files.newWriter(new File(CommonUtil.ensurePathExist(trimFileName())),
					Charset.defaultCharset());
			for (List<AvProxy> proxies : data.values()) {
				for (AvProxy avProxy : proxies) {
					bufferedWriter.write(avProxy.getIp() + ":" + avProxy.getPort());
					bufferedWriter.newLine();
				}
			}
		} catch (IOException e) {// 发生异常打印日志,但是不抛异常,因为不会影响正常逻辑
			logger.error("error when serialize proxy data", e);
		} finally {
			IOUtils.closeQuietly(bufferedWriter);
		}
	}

	@Override
	public Map<String, List<AvProxy>> unSerializeProxy() {
		Map<String, List<AvProxy>> ret = Maps.newHashMap();
		if (!new File(trimFileName()).exists()) {
			return ret;
		}
		try {
			List<String> proxies = Files.readLines(new File(trimFileName()), Charset.defaultCharset());
			List<AvProxy> avProxies = Lists.newLinkedList();
			for (String proxyString : proxies) {
				AvProxy avProxy = new AvProxy();
				String tmpString[] = proxyString.split(":");
				avProxy.setIp(tmpString[0]);
				avProxy.setPort(new Integer(tmpString[1]));
				avProxies.add(avProxy);
			}
			ret.put("", avProxies);
		} catch (Exception e) {
			logger.error("error when unSerializeProxy proxy data", e);
		}
		return ret;
	}

	/**
	 * 调整文件路径,如果为绝对路径,则使用绝对路径,否则以classPath作为根目录,而不以运行目录作为文件路径
	 *
	 * @return 调整后的文件路径
	 */
	private String trimFileName() {
		if (StringUtils.isEmpty(dumpFileName)) {
			dumpFileName = ProxyConstant.DEFAULT_PROXY_SERALIZER_FILE_VALUE;
		}
		if (dumpFileName.startsWith("/") || dumpFileName.charAt(1) == ':') {
			return dumpFileName;
		}
		String classPath = PlainTextFileAvProxyDumper.class.getResource("/").getFile();
		return new File(classPath, dumpFileName).getAbsolutePath();
	}
}
