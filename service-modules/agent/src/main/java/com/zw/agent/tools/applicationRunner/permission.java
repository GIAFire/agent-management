package com.zw.agent.tools.applicationRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)      // 指定注解可用位置
@Retention(RetentionPolicy.RUNTIME)
public @interface permission {
    String value() default "";
}
