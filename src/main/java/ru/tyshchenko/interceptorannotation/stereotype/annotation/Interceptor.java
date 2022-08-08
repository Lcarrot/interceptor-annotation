package ru.tyshchenko.interceptorannotation.stereotype.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Component
@Bean
public @interface Interceptor {
  @AliasFor(annotation = Component.class)
  String value() default "";
  String[] pathPatterns() default {"/**"};
  String[] excludePathPatterns() default {""};
}