package com.zw.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class ApiLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long costTime = System.currentTimeMillis() - startTime;

            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();

            String url = StringUtils.hasText(queryString)
                    ? uri + "?" + queryString
                    : uri;

            int status = response.getStatus();

            log.info("接口调用日志 method={}, url={}, status={}, cost={}ms",
                    method,
                    url,
                    status,
                    costTime);
        }
    }
}