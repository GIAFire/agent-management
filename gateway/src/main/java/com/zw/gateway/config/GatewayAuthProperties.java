package com.zw.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {
    private List<String> whiteList = new ArrayList<>(List.of(
            "/api/auth/auth/login",
            "/actuator/**"
    ));

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }
}
