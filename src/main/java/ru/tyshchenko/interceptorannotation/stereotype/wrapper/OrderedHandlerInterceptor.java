package ru.tyshchenko.interceptorannotation.stereotype.wrapper;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerInterceptor;

public interface OrderedHandlerInterceptor extends HandlerInterceptor {

  default int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
