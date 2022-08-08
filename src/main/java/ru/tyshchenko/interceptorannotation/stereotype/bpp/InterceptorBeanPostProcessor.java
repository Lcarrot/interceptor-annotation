package ru.tyshchenko.interceptorannotation.stereotype.bpp;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;
import ru.tyshchenko.interceptorannotation.stereotype.annotation.Interceptor;

@Component
public class InterceptorBeanPostProcessor implements BeanPostProcessor {

  private final Map<String, Advice> annotationsByBeanName = new ConcurrentHashMap<>();

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    Interceptor annotation = bean.getClass().getAnnotation(Interceptor.class);
    if (annotation != null && bean instanceof HandlerInterceptor) {
      Advice advice = new InvocationHandlerHandlerInterceptor(annotation.pathPatterns(),
          annotation.excludePathPatterns(), (HandlerInterceptor) bean
      );
      annotationsByBeanName.put(beanName, advice);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (annotationsByBeanName.containsKey(beanName)) {
      ProxyFactory proxyFactory = new ProxyFactory();
      proxyFactory.setInterfaces(HandlerInterceptor.class);
      proxyFactory.addAdvice(annotationsByBeanName.get(beanName));
      return proxyFactory.getProxy();
    }
    return bean;
  }

  private static class InvocationHandlerHandlerInterceptor implements MethodInterceptor {

    private static final PathMatcher pathMatcher = new AntPathMatcher();
    private final HandlerInterceptor handlerInterceptor;
    private final PathHelper[] include;
    private final PathHelper[] exclude;

    public InvocationHandlerHandlerInterceptor(String[] pathPatterns, String[] excludePathPatterns,
        HandlerInterceptor handlerInterceptor) {
      this.include = parsePatterns(pathPatterns);
      this.exclude = parsePatterns(excludePathPatterns);
      this.handlerInterceptor = handlerInterceptor;
    }

    private PathHelper[] parsePatterns(String[] patterns) {
      return Arrays.stream(patterns)
          .filter(pattern -> pattern != null && !pattern.isEmpty())
          .map(PathHelper::new)
          .toArray(PathHelper[]::new);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      Method method = invocation.getMethod();
      Object[] args = invocation.getArguments();
      Optional<HttpServletRequest> request = getHttpServletRequest(args);
      if (request.isEmpty() || matches(request.get())) {
        return method.invoke(handlerInterceptor, args);
      }
      return invokeDefaultMethod(invocation);
    }

    private Optional<HttpServletRequest> getHttpServletRequest(Object[] args) {
      return Arrays.stream(args)
          .filter(arg -> arg instanceof HttpServletRequest)
          .map(arg -> (HttpServletRequest) arg)
          .findFirst();
    }

    private boolean matches(HttpServletRequest request) {
      Object path = ServletRequestPathUtils.getCachedPath(request);
      for (PathHelper pathHelper : exclude) {
        if (pathHelper.matches(path, pathMatcher)) {
          return false;
        }
      }
      for (PathHelper pathHelper : include) {
        if (pathHelper.matches(path, pathMatcher)) {
          return true;
        }
      }
      return false;
    }

    private Object invokeDefaultMethod(MethodInvocation invocation) throws Throwable {
      Method method = invocation.getMethod();
      Object[] args = invocation.getArguments();
      return MethodHandles.lookup()
          .findSpecial(
              method.getDeclaringClass(),
              method.getName(),
              MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
              method.getDeclaringClass()
          )
          .bindTo(new HandlerInterceptor(){})
          .invokeWithArguments(args);
    }

    private static class PathHelper {

      private final String pattern;
      private PathPattern pathPattern;

      private PathHelper(String pattern) {
        this.pattern = pattern;
        try {
          pathPattern = PathPatternParser.defaultInstance.parse(pattern);
        } catch (PatternParseException var3) {
          //ignore
        }
      }

      public boolean matches(Object path, PathMatcher pathMatcher) {
        if (path instanceof PathContainer container) {
          if (this.pathPattern != null) {
          return this.pathPattern.matches(container);
        }
        String lookupPath = container.value();
        path = UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
      }
        return pathMatcher.match(this.pattern, (String) path);
      }
    }
  }
}
