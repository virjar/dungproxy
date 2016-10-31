package com.virjar.dungproxy.server.crawler.extractor;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/*
 * xml 模板解析器，通过输入的xml规则描述文件，抽取目标html文档的元素信息，返回一个JSON字符串 xml元素解释： name属性：在当前作用域内节点标识，下一级默认scope，同时提取成功的JSON串的key值为由name定义
 * xpath:描述当前节点的位置或者值，（如当前节点不是最终节点，那么该xpath描述节点定位规则，如果当前节点没有子节点，那么描述的是一个文本值），xpath使用htmlcleaner解析，
 * 请使用htmlcleaner支持的xpath语法 regex:文本抽取规则，为正则表达式，底层调用java正则，故请使用java支持的正则语法，另外规则使用group 1，如果作全文匹配，请使用圆括号包围
 * regex将会作为数据是否提取成功的标识 test:测试数据是否提取成功，如果没有成功，test包围的属性将不会被录入。如果提取成功，数据将会设置到test的scope中，同时后续相同group的test将会被忽略，测试失败条件是：
 * property的require属性不为true（默认为false），或者xpath没有成功提取，如果存在正则表达式，那么正则表达式提取或者匹配成功
 * group：用在test中，表示test的分组，默认group为0，在相同group中，如果有一个提取成功，那么后续提取方案将会被忽略。
 * scope：值存储空间，或者作用域。为一个树形结构关系，默认每一个匹配单元的scope由父节点的name定义，提取成功后的数据将会存放在scope中，fetch节点（文档根节点）的默认name为page，
 * 也及在默认情况下提取单元可以强行将scope设置为page，那么对于数据将会存放在全局中，供其它模块调用 fetch:默认为false，如果为true，将会收集本作用域的值，并在fetch方法的返回值的存入
 * check:如果设置check熟悉为true，那么本条记录仅仅是作为上下文判断的一个标记，提取的值不会存入。check主要设计为和test，value配合，三则配合可以实现存入xml自定义的值。
 * 如：html中对于性别的定义是男、女。我想将最终结果中录入的性别设置为美女，帅哥。那么可以使用如下代码 <test group="3543654"> <property name="genderCheck" check="true"
 * xpath="xxx" regex=".*(男).*"/> <property name="gender" value="帅哥"/> </test> 这样，如果按照html中抽取出了男这个词，将会在最终结果集中写入”帅哥“
 * value:用户自定义value，如果用户指定了value，那么解析模块将不会执行，直接采纳value值 备注：由于类似网页的布局可能存在局部差异，所以一个特定的模板可能不能完整的提取所有数据，为提高数据完整性，可以通过以下方法
 * 1.test元素，test要给group，如果group提取失败，那么将会忽略本次提取，数据将会由后续group定义。可以为数据设置多个test group
 * 2.使用正则表达式，由于api对xpath支持不完整，故xpath对元素的定位可能存在误差，可以通过正则表达式验证提取数据内容，进而影响test的成功性。
 * 3.同一个属性多个模板，数据提取采用覆盖机制，如果生一个匹配单元存入某个属性，后续遇到相同属性值的匹配单元，那么上一个属性的值将会被后一个覆盖（但是如果上一个属性的值不为null，后一个属性值为null，那么将会放弃覆盖）
 */
public class XmlModeFetcher {

    private Document doc;
    private HashMap<String, Pattern> pattenSpace;
    private HashMap<String, ClassFetcher> classFetchers = new HashMap<String, ClassFetcher>();
    private Logger logger = Logger.getLogger(XmlModeFetcher.class);

    public XmlModeFetcher(File xmlfile) throws IOException, DocumentException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(xmlfile)));
        StringBuilder sb = new StringBuilder();
        String temp = "";
        while ((temp = br.readLine()) != null) {
            sb.append(temp);
        }
        br.close();
        init(sb.toString());
    }

    private void init(String xml) throws DocumentException {
        doc = DocumentHelper.parseText(xml);
        pattenSpace = new HashMap<String, Pattern>();
        Pattern regPattern = Pattern.compile("regex=['\"](.+?)['\"]");
        Matcher regMatcher = regPattern.matcher(xml);
        while (regMatcher.find()) {
            String strReg = regMatcher.group(1);
            strReg = strReg.replaceAll("&amp;", "&");
            strReg = strReg.replaceAll("&quot;", "\"");
            if (!pattenSpace.containsKey(strReg)) {
                // System.out.println("compile regex:"+strReg);
                logger.info("compile regex:" + strReg);
                pattenSpace.put(strReg, Pattern.compile(strReg));
            }
        }
    }

    public XmlModeFetcher(String xml) throws DocumentException {
        init(xml);
    }

    public List<String> fetch(String html) {
        List<String> container = new ArrayList<String>();
        TagNode tagNodeRoot = new HtmlCleaner().clean(html);
        if (tagNodeRoot == null) {
            return null;
        }
        NodeData nodDataRoot = new NodeData(null, "page");
        innerFetch(container, doc.getRootElement(), nodDataRoot, nodDataRoot, tagNodeRoot, null);
        return container;
    }

    private boolean setNodeDataValue(String name, String value, NodeData node, String scope,
            HashMap<String, ScopeValue> failedValues) {
        if (node == null) {

            if (failedValues != null) {// set a failed value to failed containers
                if (!failedValues.containsKey(name) || failedValues.get(name) == null) {
                    failedValues.put(name, new ScopeValue(value, scope));
                }
            }
            return false;
        }

        if (node.getNodeName().equals(scope)) {
            if (node.getProperties().containsKey(name) && node.getProperties().get(name) != null && value == null)
                return true;
            node.getProperties().put(name, value);
            return true;
        } else {
            return setNodeDataValue(name, value, node.getParent(), scope, failedValues);
        }
    }

    private String getNodeDataValue(String key, NodeData node) {
        if (node == null)
            return null;
        if (node.getProperties().containsKey(key)) {
            return node.getProperties().get(key);
        } else {
            return getNodeDataValue(key, node.getParent());
        }
    }

    private boolean innerFetch(List<String> container, Element element, NodeData parent, NodeData root, TagNode tagnode,
            HashMap<String, ScopeValue> failedValue) {
        String xpath = element.attributeValue("xpath");
        String name = element.attributeValue("name");
        String scope = element.attributeValue("scope", parent.getNodeName());
        String fetch = element.attributeValue("fetch", "false");
        String regex = element.attributeValue("regex");
        String require = element.attributeValue("require", "false");
        String group = element.attributeValue("group", "0");
        String check = element.attributeValue("check", "false");
        String value = element.attributeValue("value");
        String decoder = element.attributeValue("decoder");
        String classfetcher = element.attributeValue("classfetcher");
        if (classfetcher != null) {
            TagNode classfetchnode = tagnode;
            if (xpath != null) {
                try {
                    Object Objs[] = tagnode.evaluateXPath(xpath);
                    for (Object obj : Objs) {
                        if (obj instanceof TagNode) {
                            classfetchnode = (TagNode) obj;
                            break;
                        }
                    }
                } catch (XPatherException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (!this.classFetchers.containsKey(classfetcher)) {
                try {
                    classFetchers.put(classfetcher, (ClassFetcher) XmlModeFetcher.class.getClassLoader()
                            .loadClass(classfetcher).getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return false;
                }
            }
            ClassFetcher fetcher = classFetchers.get(classfetcher);
            String ret = fetcher.fetcher(classfetchnode, 0);

            if (ret != null) {
                if ("true".equals(check)) {
                    return true;
                }
            } else {
                if ("true".equals(require)) {
                    return false;
                }
            }
            setNodeDataValue(name, ret, parent, scope, failedValue);
            return true;
        }

        // System.out.println("fetch:"+name);
        if (regex != null) {
            regex = regex.replaceAll("&amp;", "&");
            regex = regex.replaceAll("&quot;", "\"");
        }

        if (element.getName().equals("fetch")) {// root
            Iterator<Element> it = element.elementIterator();
            while (it.hasNext()) {
                Element childElement = it.next();
                // NodeData nodedata = new NodeData(parent,name);
                String rootscope = element.attributeValue("name", "page");
                parent.nodeName = rootscope;
                innerFetch(container, childElement, parent, root, tagnode, null);
            }
        } else if ("test".equals(element.getName())) {
            // if group fetch is successful
            if (parent.getGroupStates().containsKey(group) && parent.getGroupStates().get(group)) {
                return true;
            }
            if (element.nodeCount() == 0) {
                parent.getGroupStates().put(group, true);
                return true;
            }
            Iterator<Element> it = element.elementIterator();

            // new a temporary environment
            List<String> tempcontainer = new ArrayList<String>();
            NodeData temproot = new NodeData(null, scope);
            boolean istestSuccessful = true;
            HashMap<String, ScopeValue> failedValues = new HashMap<String, ScopeValue>();
            while (it.hasNext()) {
                Element childElement = it.next();
                // NodeData nodedata = new NodeData(parent,name);
                if (!innerFetch(tempcontainer, childElement, temproot, temproot, tagnode, failedValues)) {
                    istestSuccessful = false;
                    break;
                }

            }

            if (istestSuccessful) {
                parent.getGroupStates().put(group, true);
                for (String jsonItem : tempcontainer) {
                    container.add(jsonItem);
                }

                Iterator<Entry<String, ScopeValue>> itfailedValues = failedValues.entrySet().iterator();
                while (itfailedValues.hasNext()) {
                    Entry<String, ScopeValue> itemvalue = itfailedValues.next();
                    setNodeDataValue(itemvalue.getKey(), itemvalue.getValue().value, parent, itemvalue.getValue().scope,
                            null);
                }

                // Iterator<Entry<String, String>> tempit = temproot.properties.entrySet().iterator();
                parent.properties.putAll(temproot.getProperties());

            }

        } else if (element.nodeCount() == 0) {
            if (value != null) {
                setNodeDataValue(name, value, parent, scope, failedValue);
                return true;
            }
            if (xpath == null && name == null) {
                return false;
            }

            if (xpath == null) {
                String text = getNodeDataValue(name, parent);
                if ("true".equals(require) && text == null)
                    return false;
                setNodeDataValue(name, text, parent, scope, failedValue);
                return true;
            }

            try {

                Object textObjs[] = tagnode.evaluateXPath(xpath);
                if (textObjs.length == 0) {
                    // logger.info("fetch failed for xpath:"+xpath+" parent
                    // xpath:"+element.getParent().attributeValue("xpath",""));
                    if ("true".equals(require) || "true".equals(check))
                        return false;
                    return true;
                }
                String text = textObjs[0].toString().trim();
                // System.out.println("before regex:"+text);
                // System.out.println("regex:"+regex);

                if (regex != null) {
                    Pattern pattern = pattenSpace.get(regex);
                    if (pattern != null) {
                        Matcher matcher = pattern.matcher(text);
                        if (matcher.find()) {
                            text = matcher.group(1);
                        } else {
                            text = null;
                            if ("true".equals(require))
                                return false;
                        }
                    }

                }

                if (decoder != null)
                    text = Decoder.decode(text, decoder);
                // System.out.println("end regex:"+text);
                // store value
                if ("true".equals(check)) {
                    return true;
                }
                setNodeDataValue(name, text, parent, scope, failedValue);
            } catch (XPatherException e) {
                // TODO Auto-generated catch block
                // logger.info("fetch failed for xpath:"+xpath+" parent
                // xpath:"+element.getParent().attributeValue("xpath",""));
                logger.error(e, e);
                e.printStackTrace();
                return true;
            }
            return true;
        } else {// combine

            Object[] selfNode = null;
            try {
                // logger.info("get self node ,relative xpath is:"+xpath);
                if (xpath.equals("./../") || xpath.equals("../")) {
                    selfNode = new Object[1];
                    selfNode[0] = tagnode.getParent();
                } else {
                    selfNode = tagnode.evaluateXPath(xpath);
                }
                // logger.info("node number:"+selfNode.length);
                JSONArray jsonArray = new JSONArray();
                fetchFailed: for (Object obj : selfNode) {
                    if (obj instanceof TagNode) {
                        Iterator<Element> it = element.elementIterator();
                        NodeData nodedata = new NodeData(parent, name);
                        while (it.hasNext()) {
                            Element childElement = it.next();
                            if (!innerFetch(container, childElement, nodedata, root, (TagNode) obj, null))
                                continue fetchFailed;
                        }
                        // store json string value to container
                        Set<Entry<String, String>> dataset = nodedata.getProperties().entrySet();
                        Iterator<Entry<String, String>> entryit = dataset.iterator();
                        // JSONObject jsonObject = new JSONObject(nodedata.getProperties());//why ?????
                        JSONObject jsonObject = new JSONObject();
                        while (entryit.hasNext()) {
                            Entry<String, String> entry = entryit.next();
                            /*
                             * if(entry.getValue()==null){ entry.setValue(""); }
                             */
                            jsonObject.put(entry.getKey(), entry.getValue());

                        }
                        jsonArray.add(jsonObject);
                        if (fetch.equals("true")) {
                            container.add(jsonObject.toJSONString());
                        }
                    }

                }
                String jsonArrayStr = jsonArray.toJSONString();
                if (!"[{}]".equals(jsonArrayStr) && !"[]".equals(jsonArrayStr))
                    setNodeDataValue(name, jsonArrayStr, parent, scope, failedValue);
            } catch (XPatherException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return true;
            }

        }
        return true;
    }

    private class NodeData {
        private NodeData parent;
        private String nodeName;

        private HashMap<String, String> properties;
        private HashMap<String, Boolean> groupStates;

        public NodeData(NodeData parent, String nodeName) {
            super();
            this.parent = parent;
            this.nodeName = nodeName;
            properties = new HashMap<String, String>();
            groupStates = new HashMap<String, Boolean>();
        }

        public NodeData getParent() {
            return parent;
        }

        public HashMap<String, String> getProperties() {
            return properties;
        }

        public String getNodeName() {
            return nodeName;
        }

        public HashMap<String, Boolean> getGroupStates() {
            return groupStates;
        }
    }

    private class ScopeValue {
        private String value;
        private String scope;

        public ScopeValue(String value, String scope) {
            super();
            this.value = value;
            this.scope = scope;
        }

    }
}
