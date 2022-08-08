package ru.tyshchenko.interceptorannotation.stereotype.bpp;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import ru.tyshchenko.interceptorannotation.stereotype.annotation.Interceptor;

@Component
public class InterceptorBeanPostProcessor implements BeanPostProcessor {

  private final Map<String, InvocationHandler> annotationsByBeanName = new ConcurrentHashMap<>();

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    if (bean instanceof HandlerInterceptor) {
      Interceptor annotation = bean.getClass().getAnnotation(Interceptor.class);
      InvocationHandler handler = new InvocationHandlerHandlerInterceptor(annotation.pathPatterns(),
          annotation.excludePathPatterns(), (HandlerInterceptor) bean
      );
      annotationsByBeanName.put(beanName, handler);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (annotationsByBeanName.containsKey(beanName)) {
      bean = Proxy.newProxyInstance(bean.getClass().getClassLoader(),
          new Class[] {HandlerInterceptor.class}, annotationsByBeanName.get(beanName)
      );
    }
    return bean;
  }

  private static class InvocationHandlerHandlerInterceptor implements InvocationHandler {

    private final MappedInterceptor mappedInterceptor;

    public InvocationHandlerHandlerInterceptor(String[] pathPatterns, String[] excludePathPatterns,
        HandlerInterceptor handlerInterceptor) {
      this.mappedInterceptor = new MappedInterceptor(pathPatterns, excludePathPatterns,
          handlerInterceptor
      );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Optional<HttpServletRequest> request = Arrays.stream(args)
          .filter(arg -> arg instanceof HttpServletRequest)
          .map(arg -> (HttpServletRequest) arg)
          .findFirst();
      if (request.isEmpty() || mappedInterceptor.matches(request.get())) {
        return method.invoke(mappedInterceptor.getInterceptor(), args);
      }
      return MethodHandles.lookup()
          .findSpecial(
              method.getDeclaringClass(),
              method.getName(),
              MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
              method.getDeclaringClass()
          )
          .bindTo(proxy)
          .invokeWithArguments(args);
    }
  }
}
