package ru.tyshchenko.interceptorannotation.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.java.Log;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.tyshchenko.interceptorannotation.stereotype.annotation.Interceptor;

@Log
@Interceptor(pathPatterns = "/hello")
public class LogInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    log.info("Handle Request");
   return true;
  }
}
