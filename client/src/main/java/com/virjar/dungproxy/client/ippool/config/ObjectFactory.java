package com.virjar.dungproxy.client.ippool.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.virjar.dungproxy.client.ippool.exception.ObjectCreateException;

/**
 * Created by virjar on 16/9/30.
 */
public class ObjectFactory {
    private static final Logger logger = LoggerFactory.getLogger(ObjectFactory.class);

    public static <T> T newInstance(String className) {
        Preconditions.checkNotNull(className);
        try {
            Class<?> aClass = Class.forName(className);
            return (T) aClass.newInstance();
        } catch (Exception e) {
            logger.error("can not create instance for class :{}", className);
            throw new ObjectCreateException("can not create instance for class " + className, e);
        }
    }
}
