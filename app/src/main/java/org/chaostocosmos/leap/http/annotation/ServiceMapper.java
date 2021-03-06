package org.chaostocosmos.leap.http.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ServletDescriptor annotation
 * 
 * @author 9ins
 * @since 2021.09.17
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceMapper {    
    /**
     * Servlet context path
     * @return
     */
    String path() default "";
}
