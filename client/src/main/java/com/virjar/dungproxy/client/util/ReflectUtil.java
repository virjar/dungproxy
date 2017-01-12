package com.virjar.dungproxy.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Preconditions;

/**
 * 为了兼容WebMagic的版本问题引入的工具类,通过反射来检查版本并调用当前版本不存在的API
 */
public class ReflectUtil {

    public static void addField(Object obj, String key, Object value)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(key);
        field.setAccessible(true);
        field.set(obj, value);
    }

    public static <T> T getField(Object obj, String key)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Class clazz = obj.getClass();
        Field field = null;
        do {
            try {
                field = clazz.getDeclaredField(key);
                field.setAccessible(true);
                return (T) field.get(obj);
            } catch (NoSuchFieldException e) {
                // do nothing
            }
        } while ((clazz = clazz.getSuperclass()) != null);
        return null;
    }

    public static <T> T invoke(Object obj, String methodName, Class[] clazzList, Object[] args) {
        Preconditions.checkState(clazzList.length == args.length, "参数签名和参数数目不一致");
        Class<?> clazz = obj.getClass();
        Method method = null;
        do {
            try {
                method = clazz.getMethod(methodName, clazzList);
                return (T) method.invoke(obj, args);
            } catch (NoSuchMethodException e) {
                // do nothing
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(obj.getClass() + ":调用方法:" + methodName + "失败");
            }

        } while ((clazz = clazz.getSuperclass()) != null);
        throw new IllegalStateException(obj.getClass() + ":找不到方法:" + methodName);
    }

    public static boolean hasMethod(Object obj,String methodName){
        Method[] methods = obj.getClass().getMethods();
        for(Method method:methods){
            if(method.getName().equalsIgnoreCase(methodName)){
                return true;
            }
        }
        return false;
    }
}
