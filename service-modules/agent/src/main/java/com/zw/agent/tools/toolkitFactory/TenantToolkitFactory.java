package com.zw.agent.tools.toolkitFactory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.genai.types.ToolConfig;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.service.AiToolInfoConfigService;
import com.zw.agent.tools.applicationRunner.ToolRegistrySyncRunner;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.baomidou.mybatisplus.extension.spi.SpringCompatibleSet.applicationContext;

@Component
@RequiredArgsConstructor
public class TenantToolkitFactory {

    private static final Logger log = LoggerFactory.getLogger(TenantToolkitFactory.class);

    private final AiToolInfoConfigService toolInfoConfigService;

    public Toolkit buildToolkit(Long tenantId) {
        Toolkit toolkit = new Toolkit();

        List<AiToolInfoConfigEntity> toolList = toolInfoConfigService.list(new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                .eq(AiToolInfoConfigEntity::getTenantId, tenantId));

        for (AiToolInfoConfigEntity toolConfig : toolList) {
            Object toolBean = applicationContext.getBean(toolConfig.getBeanName());

            if (toolBean == null) {
                toolkit.registerTool(null);
                log.error("beanName:{} not found", toolConfig.getId());
            }
            toolkit.registerTool(toolBean);
        }
        return toolkit;
    }
}