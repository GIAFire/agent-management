package com.zw.agent.factory.toolkitFactory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.service.AiToolInfoConfigService;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TenantToolkitFactory {

    private static final Logger log = LoggerFactory.getLogger(TenantToolkitFactory.class);

    private final AiToolInfoConfigService toolInfoConfigService;
    private final ApplicationContext applicationContext;

    public Toolkit buildToolkit(Long tenantId) {
        Toolkit toolkit = new Toolkit();

        List<AiToolInfoConfigEntity> toolList = toolInfoConfigService.list(new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                .eq(AiToolInfoConfigEntity::getTenantId, tenantId)
                .eq(AiToolInfoConfigEntity::getEnabled, true)
                .orderByAsc(AiToolInfoConfigEntity::getClassName)
                .orderByAsc(AiToolInfoConfigEntity::getMethodName));

        Set<String> registeredClasses = new LinkedHashSet<>();
        for (AiToolInfoConfigEntity toolInfo : toolList) {
            if (!StringUtils.hasText(toolInfo.getClassName())
                    || !registeredClasses.add(toolInfo.getClassName())) {
                continue;
            }

            Object toolBean = resolveToolBean(toolInfo);
            if (toolBean == null) {
                log.error("Tool bean not found, toolId={}, beanName={}, className={}",
                        toolInfo.getId(), toolInfo.getBeanName(), toolInfo.getClassName());
                continue;
            }
            toolkit.registerTool(toolBean);
        }
        return toolkit;
    }

    private Object resolveToolBean(AiToolInfoConfigEntity toolConfig) {
        try {
            if (StringUtils.hasText(toolConfig.getBeanName())
                    && applicationContext.containsBean(toolConfig.getBeanName())) {
                return applicationContext.getBean(toolConfig.getBeanName());
            }

            Class<?> toolClass = ClassUtils.forName(toolConfig.getClassName(), applicationContext.getClassLoader());
            String[] beanNames = applicationContext.getBeanNamesForType(toolClass, false, false);
            if (beanNames.length > 0) {
                return applicationContext.getBean(beanNames[0]);
            }

            return applicationContext.getAutowireCapableBeanFactory().createBean(toolClass);
        } catch (ClassNotFoundException | LinkageError | BeansException ex) {
            log.error("Failed to resolve tool bean, beanName={}, className={}",
                    toolConfig.getBeanName(), toolConfig.getClassName(), ex);
            return null;
        }
    }
}
