package com.checkout.payment.gateway.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Request-ID";
  public static final String MDC_KEY = "request_id";

  private static final Logger LOG = LoggerFactory.getLogger(RequestIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    long startedAt = System.nanoTime();
    MDC.put(MDC_KEY, requestId);
    response.setHeader(HEADER_NAME, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
      LOG.info("event=request_completed method={} path={} status={} duration_ms={}",
          request.getMethod(), request.getRequestURI(), response.getStatus(), durationMillis);
      MDC.remove(MDC_KEY);
    }
  }
}
