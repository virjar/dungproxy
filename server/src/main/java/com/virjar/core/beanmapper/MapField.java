package com.virjar.core.beanmapper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for field mapping
 * <p>
 * The mapping value/currentField can support {@link java.util.List List}, {@link java.util.Map Map}, and even the
 * nested object
 * <p>
 * {@link java.util.List List} e.g. fieldParts[0]
 * <p>
 * {@link java.util.Map Map} e.g. fieldParts['key']
 * <p>
 * {@link Object} e.g. object.field
 * 
 * @author weijia.deng
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapField {
    public static final String MAP_DELIMITERS = "=";

    public static final String MULTI_MAP_DELIMITERS = ",";

    /**
     * the mapping field
     * 
     * @return
     */
    String value() default "";

    /**
     * make to support list, array, object mapping
     * <p>
     * format: complexMap="aClassField[0]=bClassFileda,fielda[1]=bClassFiledb"
     * 
     * @return
     */
    String complexMap() default "";

}