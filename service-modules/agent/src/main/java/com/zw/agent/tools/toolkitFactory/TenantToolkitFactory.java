package com.zw.agent.tools.toolkitFactory;

import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TenantToolkitFactory {

    private final Map<String, Object> allToolBeans;
    private final TenantToolConfigService tenantToolConfigService;


    public Toolkit buildToolkit(String tenantId) {
        Toolkit toolkit = new Toolkit();

        List<String> allowedToolBeanNames =
                tenantToolConfigService.getAllowedToolBeans(tenantId);

        for (String beanName : allowedToolBeanNames) {
            Object toolBean = allToolBeans.get(beanName);

            if (toolBean == null) {
                throw new IllegalArgumentException("Unknown tool bean: " + beanName);
            }

            // 只注册当前租户允许的 tool
            toolkit.registerTool(toolBean);
        }

        return toolkit;
    }
}