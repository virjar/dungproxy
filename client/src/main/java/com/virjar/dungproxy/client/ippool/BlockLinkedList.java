package com.virjar.dungproxy.client.ippool;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by virjar on 17/5/20.<br/>
 * 增加一个take方法,当IP池为空的时候,阻塞等待
 * 
 * @since 0.0.6
 * @author virjar
 */
public class BlockLinkedList<T> extends LinkedList<T> {
    private static Logger logger = LoggerFactory.getLogger(BlockLinkedList.class);
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    private void notifyIfNotEmpty() {
        if (size() > 0 && size() < 3) {
            try {
                lock.lock();
                condition.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean add(T t) {
        lock.lock();
        try {
            boolean ret = super.add(t);
            notifyIfNotEmpty();
            return ret;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void add(int index, T element) {
        lock.lock();
        try {
            if (index >= size()) {
                super.addLast(element);
            } else {
                super.add(index, element);
            }
            notifyIfNotEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        lock.lock();
        try {
            boolean ret = super.addAll(c);
            notifyIfNotEmpty();
            return ret;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        lock.lock();
        try {
            boolean ret = super.addAll(index, c);
            notifyIfNotEmpty();
            return ret;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addFirst(T t) {
        lock.lock();
        try {
            super.addFirst(t);
            notifyIfNotEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addLast(T t) {
        lock.lock();
        try {
            super.addLast(t);
            notifyIfNotEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(T t) {
        lock.lock();
        try {
            boolean ret = super.offer(t);
            notifyIfNotEmpty();
            return ret;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offerFirst(T t) {
        lock.lock();
        try {
            boolean ret = super.offerFirst(t);
            notifyIfNotEmpty();
            return ret;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offerLast(T t) {
        lock.lock();
        try {
            boolean ret = super.offerLast(t);
            notifyIfNotEmpty();
            return ret;
        } finally {
            lock.unlock();
        }
    }

    public T take(long time, TimeUnit timeUnit) {
        T t = poll();
        while (t == null) {
            try {
                lock.lock();
                try {
                    if (time <= 0) {
                        condition.await();
                    } else {
                        condition.await(time, timeUnit);
                    }
                } catch (InterruptedException e) {
                    logger.error("error when await new element", e);
                    return null;
                }
            } finally {
                lock.unlock();
            }
            t = poll();
        }
        return t;
    }

    public T take() {
        return take(0, null);
    }

    @Override
    public T get(int index) {
        lock.lock();
        try {
            return super.get(index);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T getFirst() {
        lock.lock();
        try {
            return super.getFirst();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T getLast() {
        lock.lock();
        try {
            return super.getLast();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T peek() {
        lock.lock();
        try {
            return super.peek();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T peekFirst() {
        lock.lock();
        try {
            return super.peekFirst();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T peekLast() {
        lock.lock();
        try {
            return super.peekLast();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T poll() {
        lock.lock();
        try {
            return super.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T pollFirst() {
        lock.lock();
        try {
            return super.pollFirst();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T pollLast() {
        lock.lock();
        try {
            return super.pollLast();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T pop() {
        lock.lock();
        try {
            return super.pop();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void push(T t) {
        lock.lock();
        try {
            super.push(t);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T remove() {
        lock.lock();
        try {
            return super.remove();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T remove(int index) {
        lock.lock();
        try {
            return super.remove(index);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        lock.lock();
        try {
            return super.remove(o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T removeFirst() {
        lock.lock();
        try {
            return super.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T removeLast() {
        lock.lock();
        try {
            return super.removeLast();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        lock.lock();
        try {
            return super.removeFirstOccurrence(o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        lock.lock();
        try {
            return super.removeLastOccurrence(o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T set(int index, T element) {
        lock.lock();
        try {
            return super.set(index, element);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T element() {
        lock.lock();
        try {
            return super.element();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            super.clear();
        } finally {
            lock.unlock();
        }
    }
}
