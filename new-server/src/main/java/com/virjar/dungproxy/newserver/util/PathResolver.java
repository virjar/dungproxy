package com.virjar.dungproxy.newserver.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by virjar on 2018/5/18.
 */
public class PathResolver {
    private static final Pattern urlPattern = Pattern.compile("https?://([^/]+)(.*)");
    private static final Pattern fileNamePattern = Pattern.compile("https?://([^/]+).*/(.+)");
    private static final Splitter dotSplitter = Splitter.on(".").omitEmptyStrings().trimResults();
    private static final Joiner filePathJoiner = Joiner.on("/").skipNulls();
    private static final Splitter urlSeparatorSplitter = Splitter.on("/").omitEmptyStrings().trimResults();

    private static String dealWithHomeFlag(String input) {
        return StringUtils.startsWith(input, "~") ? new File(System.getProperty("user.home"), input.substring(1)).getAbsolutePath() : input;
    }

    public static String resolveAbsolutePath(String pathName) {
        if (pathName == null) {
            return null;
        }
        // file protocol
        if (StringUtils.startsWithIgnoreCase(pathName, "file:")) {
            return dealWithHomeFlag(pathName.substring("file:".length()));
        }

        // think as normal file
        File tempFile = new File(dealWithHomeFlag(pathName));
        if (tempFile.exists()) {
            return tempFile.getAbsolutePath();
        }

        if (StringUtils.startsWithIgnoreCase(pathName, "classpath:")) {
            pathName = pathName.substring("classpath:".length());
            // as classpath
            URL url = PathResolver.class.getResource(pathName);
            if (url != null) {
                return new File(url.getFile()).getAbsolutePath();
            } else {
                URL classPathDirectoryRoot = PathResolver.class.getResource("/");
                if (classPathDirectoryRoot == null) {
                    return pathName;
                }
                return new File(new File(classPathDirectoryRoot.getFile()).getAbsoluteFile(), pathName)
                        .getAbsolutePath();
            }

        }

        // as classpath
        URL url = PathResolver.class.getResource(pathName);
        if (url != null) {
            return new File(url.getFile()).getAbsolutePath();
        }
        return dealWithHomeFlag(pathName);
    }

    /**
     * calculate a file path to save http url resource, base rule is "basePath +revert domain+ path"
     *
     * @param basePath 起始路径
     * @param url      url ,http://sss/path/resource.html
     * @return path
     */
    public static String commonDownloadPath(String basePath, String url) {
        basePath = dealWithHomeFlag(basePath);
        Matcher matcher = urlPattern.matcher(url);
        if (!matcher.find()) {
            return new File(basePath, url).getAbsolutePath();
        }
        String domain = matcher.group(1);
        String resource = matcher.group(2);

        List<String> reverse = Lists.newLinkedList(Lists.reverse(dotSplitter.splitToList(domain)));
        List<String> resources = urlSeparatorSplitter.splitToList(resource);
        if (resources.size() > 0) {
            String fileName = resources.get(resources.size() - 1);//最后一个代表文件名
            if (StringUtils.contains(fileName, "#")) {
                //有锚点,需要干掉锚点
                String newFileName = fileName.substring(0, fileName.indexOf("#"));
                resources.remove(resources.size() - 1);
                resources.add(newFileName);
            }
            reverse.addAll(resources);
        } else {
            reverse.add("index.html");
        }

        String fileName = reverse.get(reverse.size() - 1);//最后一个代表文件名
        if (!StringUtils.contains(fileName, ".")) {
            reverse.add("index.html");
        }

        String filePath = filePathJoiner.join(reverse);
        File targetFile = new File(basePath, filePath);
        File parentFile = targetFile.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                return new File(basePath, url).getAbsolutePath();
            }
        }
        return targetFile.getAbsolutePath();
        // return absolutePath;
    }

    /**
     * calculate  a path for a url resource, use "base path" + "resource file name"<br/>
     * for example: base path is "/var/log/", resource url is: "http://sss/path/resource.html" <br/>
     * the resource file path will be:"/var/log/resource.html" <br/>
     * domain field & path field will be ignore
     *
     * @param basePath file base director,the root of  resource download directory
     * @param url      resource url ,must be http or https protocol
     * @return the absolute file path for given url
     */
    public static String onlySource(String basePath, String url) {
        basePath = dealWithHomeFlag(basePath);

        Matcher matcher = fileNamePattern.matcher(url);
        if (!matcher.find()) {
            return new File(basePath, UUID.randomUUID().toString()).getAbsolutePath();
        }
        String resource = matcher.group(2);
        File targetFile = new File(basePath, resource);
        File parentFile = targetFile.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                return new File(basePath, url).getAbsolutePath();
            }
        }
        return targetFile.getAbsolutePath();
    }

    /**
     * calculate download path for a url resource,use basePath for resource directory<br/>
     * domain information will be ignore<br/>
     * path information will be translate to under line separator<br/>
     * for example: base path is :"/var/log/"  url is:"http://www.baidu.com/1/2/3/img/resource.html"<br/>
     * final path will be:"/var/log/1_2_3_img_resource.html"
     *
     * @param basePath download root path
     * @param url      url
     * @return the absolute file path for given url
     */
    public static String sourceToUnderLine(String basePath, String url) {
        basePath = dealWithHomeFlag(basePath);
        Matcher matcher = urlPattern.matcher(url);
        if (!matcher.find()) {
            return new File(basePath, url).getAbsolutePath();
        }
        String resource = matcher.group(2);
        File targetFile = new File(basePath, Joiner.on("_").skipNulls().join(urlSeparatorSplitter.splitToList(resource)));
        File parentFile = targetFile.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                return new File(basePath, url).getAbsolutePath();
            }
        }
        return targetFile.getAbsolutePath();
    }

    public static String domainAndSource(String basePath, String url) {
        basePath = dealWithHomeFlag(basePath);

        Matcher matcher = fileNamePattern.matcher(url);
        if (!matcher.find()) {
            return new File(basePath, UUID.randomUUID().toString()).getAbsolutePath();
        }
        String domain = matcher.group(1);
        String resource = matcher.group(2);

        List<String> reverse = Lists.newLinkedList(Lists.reverse(dotSplitter.splitToList(domain)));
        reverse.add(resource);
        String filePath = filePathJoiner.join(reverse);
        File targetFile = new File(basePath, filePath);
        File parentFile = targetFile.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                return new File(basePath, url).getAbsolutePath();
            }
        }
        return targetFile.getAbsolutePath();
    }

    public static String getFileName(String path) {
        int index = path.lastIndexOf(File.separatorChar);
        if (index < 0) {
            return path;
        }
        return path.substring(index + 1);
    }
}
