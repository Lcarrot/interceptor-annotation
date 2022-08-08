package ru.tyshchenko.interceptorannotation.stereotype.configuration;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.tyshchenko.interceptorannotation.stereotype.wrapper.OrderedHandlerInterceptor;

@Configuration
public class InterceptorWebMvcConfiguration implements WebMvcConfigurer {

  private final List<HandlerInterceptor> handlerInterceptors;

  public InterceptorWebMvcConfiguration(List<HandlerInterceptor> handlerInterceptors) {
    this.handlerInterceptors = handlerInterceptors;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    handlerInterceptors.forEach(interceptor -> {
      InterceptorRegistration registration = registry.addInterceptor(interceptor);
      if (interceptor instanceof OrderedHandlerInterceptor) {
        registration.order(((OrderedHandlerInterceptor) interceptor).getOrder());
      }
    });
  }
}
