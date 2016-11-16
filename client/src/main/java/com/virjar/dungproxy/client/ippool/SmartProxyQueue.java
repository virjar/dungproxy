package com.virjar.dungproxy.client.ippool;

import java.util.concurrent.locks.ReentrantLock;

import com.virjar.dungproxy.client.model.AvProxy;

/**
 * 智能的代理管理容器。他是单独为代理IP这种打分模型设计的容器<br/>
 * Created by virjar on 16/11/15.
 */
public class SmartProxyQueue {

    private Node head = new Node();
    private Node tail = head;

    /**
     * 仅在初始化的时候使用
     * 
     * @param avProxy 增加代理
     */
    public synchronized void addProxy(AvProxy avProxy) {
        Node node = new Node();
        node.pre = tail;
        tail.after = node;
        tail = node;
        node.avProxy = avProxy;
        avProxy.setNode(node);
    }

    public AvProxy get() {
        Node after = head.after;
        if (after == null) {
            return null;
        }
        remove(after);
        return after.avProxy;
    }

    private void remove(Node node) {
        accquirWriteLock(node);
        Node pre = node.pre;
        Node next = node.after;
        pre.after = next.after;
        next.pre = pre;
        node.pre = null;
        node.after = null;
        releaseWriteLock(pre, node, next);
    }

    private void releaseWriteLock(Node pre, Node node, Node next) {

        pre.afterlLock.unlock();
        node.preLock.unlock();
        node.afterlLock.unlock();
        next.preLock.unlock();
    }

    private void accquirWriteLock(Node node) {
        while (true) {
            if (node.pre.afterlLock.tryLock()) {
                // 拿到上一个节点的写权限
                if (node.preLock.tryLock()) {
                    if (node.afterlLock.tryLock()) {
                        if (node.after == null) {
                            return;
                        }
                        if (node.after.preLock.tryLock()) {
                            return;
                        }
                        node.afterlLock.unlock();
                    }
                    node.preLock.unlock();
                }
                node.pre.afterlLock.unlock();
            }
        }
    }

    public static class Node {
        AvProxy avProxy;// 代理和node一一对应
        ReentrantLock preLock = new ReentrantLock(false);
        ReentrantLock afterlLock = new ReentrantLock(false);
        Node pre;
        Node after;
    }
}
