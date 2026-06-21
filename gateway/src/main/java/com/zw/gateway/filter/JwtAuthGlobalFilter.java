package com.zw.gateway.filter;

import com.zw.common.RedisService;
import com.zw.common.constant.RedisConstants;
import com.zw.common.constant.UserHeaderConstants;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import com.zw.common.constant.HttpStatus;
import com.zw.common.entity.Result;
import com.zw.common.utils.JwtUtils;
import com.zw.gateway.config.GatewayAuthProperties;
import com.zw.gateway.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {
    private final JwtProperties jwtProperties;
    private final GatewayAuthProperties authProperties;
    @Autowired
    private ObjectMapper objectMapper;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhiteListed(path) || "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        try {
            String token = resolveToken(exchange.getRequest());
            Map<String, Object> claims = JwtUtils.parseToken(token, jwtProperties.getSecret(), jwtProperties.getIssuer());
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(UserHeaderConstants.USER_ID, stringClaim(claims, "userId"))
                    .header(UserHeaderConstants.USER_NAME, stringClaim(claims, "userName"))
                    .header(UserHeaderConstants.TENANT_ID, stringClaim(claims, "tenantId"))
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (IllegalArgumentException e) {
            return writeUnauthorized(exchange, e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhiteListed(String path) {
        return authProperties.getWhiteList().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveToken(ServerHttpRequest request) {
        String headerValue = request.getHeaders().getFirst(jwtProperties.getHeader());
        if (headerValue == null || headerValue.isBlank()) {
            throw new IllegalArgumentException("缺少访问令牌");
        }
        String prefix = jwtProperties.getTokenPrefix();
        if (prefix != null && !prefix.isBlank()) {
            if (!headerValue.startsWith(prefix)) {
                throw new IllegalArgumentException("访问令牌格式错误");
            }
            return headerValue.substring(prefix.length()).trim();
        }
        return headerValue.trim();
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        bytes = objectMapper.writeValueAsBytes(Result.fail(HttpStatus.UNAUTHORIZED, message));
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private String stringClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
