package com.virjar.dungproxy.client.ningclient.concurrent;

/**
 * Description: ThreadRecycles
 *
 * @author lingtong.fu
 * @version 2016-09-04 12:48
 */

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadRecycles {
    private static final Logger log = LoggerFactory.getLogger(ThreadRecycles.class);
    private static final ThreadLocal<ThreadRecycles> local = new ThreadLocal<>();
    private LinkedHashMap<Object, Recyclable> recycles = new LinkedHashMap<>();

    public static ThreadRecycles init() {
        ThreadRecycles tlr = local.get();
        if (tlr == null) {
            tlr = new ThreadRecycles();
            local.set(tlr);
        }
        return tlr;
    }

    public static void release() {
        ThreadRecycles current = local.get();
        if (current != null) {
            current.clear();
            local.remove();
        }

    }

    public static Entry<Object, Recyclable>[] recycleSet() {
        ThreadRecycles current = local.get();
        if (current == null) {
            return new Entry[0];
        } else {
            Entry[] arr = new Entry[current.recycles.size()];
            current.recycles.entrySet().toArray(arr);
            return arr;
        }
    }

    public static void setRecycle(Object key, Recyclable recycle) {
        ThreadRecycles current = local.get();
        if (current == null) {
            current = init();
            log.error("线程回收器(LocalRecycles)未被成功接管,请确定当前资源是在安全的执行环境(线程池)中被开启." + recycle + ":" + Thread.currentThread());
        }

        current.put(key, recycle);
    }

    public static <E> E getRecycle(Object key) {
        ThreadRecycles current = local.get();
        return current != null ? (E) current.recycles.get(key) : null;
    }

    public static void removeRecycle(Object key) {
        ThreadRecycles current = local.get();
        if (current != null) {
            current.recycles.remove(key);
        }

    }

    private ThreadRecycles() {
    }

    private void put(Object key, Recyclable recycle) {
        if (this.recycles.containsKey(key)) {
            throw new IllegalStateException("重复的key:" + key);
        } else {
            this.recycles.put(key, recycle);
        }
    }

    private void clear() {
        Recyclable[] arr = new Recyclable[this.recycles.size()];
        this.recycles.values().toArray(arr);
        this.recycles.clear();

        for (int i = arr.length - 1; i >= 0; --i) {
            try {
                arr[i].recycle();
                log.warn("资源回收器捕获到未回收的资源: " + Thread.currentThread() + "|" + arr[i]);
            } catch (Exception e) {
                log.warn("recycle failed", e);
            }
        }

    }
}