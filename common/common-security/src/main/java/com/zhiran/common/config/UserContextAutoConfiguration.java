package com.zhiran.common.config;

import com.zhiran.common.Interceptor.UserContextInterceptor;
import com.zhiran.common.RedisService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UserContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UserContextInterceptor userContextInterceptor(RedisService redisService, JwtProperties jwtProperties) {
        return new UserContextInterceptor(redisService, jwtProperties);
    }

    @Bean
    public WebMvcConfigurer userContextWebMvcConfigurer(
            UserContextInterceptor userContextInterceptor
    ) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(userContextInterceptor)
                        .addPathPatterns("/**")
                        .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/static/**", "/error");
            }
        };
    }
}