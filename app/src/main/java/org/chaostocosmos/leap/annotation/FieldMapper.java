package org.chaostocosmos.leap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FieldMapper annotation
 * 
 * @author 9ins
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldMapper {
    /**
     * Mapping class 
     * @return
     */
    Class<? extends Object> mappingClass();

    /**
     * Mapping class parameters
     * @return
     */
    Class<? extends Object>[] parameters();

    /**
     * Whether create new instance
     */
    boolean newInstance() default true;
}
