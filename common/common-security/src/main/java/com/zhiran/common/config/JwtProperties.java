package com.zhiran.common.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ToString(exclude = "secret")
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {
    private String secret = "agentScopeJwtSecretKeyForLocalDevChangeMe20260621";
    private String issuer = "agent-scope";
    private long expireSeconds = 7 * 24 * 60 * 60;//7天过期
}
