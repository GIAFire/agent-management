package com.zw.agent.tools.applicationRunner;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.service.AiToolInfoConfigService;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ToolRegistrySyncRunner implements ApplicationRunner {

    private static final String TOOL_BASE_PACKAGE = "com.zw.agent.tools.*";
    private static final Logger log = LoggerFactory.getLogger(ToolRegistrySyncRunner.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ApplicationContext applicationContext;
    private final AiToolInfoConfigService toolInfoConfigService;

    @Override
    public void run(ApplicationArguments args) {
        List<AiToolInfoConfigEntity> toolConfigs = scanToolConfigs();

        if (toolConfigs.isEmpty()) {
            log.info("No ToolBase tools found under package {}", TOOL_BASE_PACKAGE);
            return;
        }

        int count = toolInfoConfigService.upsertBatch(toolConfigs);
        log.info("Synced {} tool definitions into ai_tool_info_config, affected rows={}",
                toolConfigs.size(), count);
    }

    private List<AiToolInfoConfigEntity> scanToolConfigs() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

        List<AiToolInfoConfigEntity> toolConfigs = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(TOOL_BASE_PACKAGE)) {
            String className = candidate.getBeanClassName();
            if (!StringUtils.hasText(className)) {
                continue;
            }

            Class<?> toolClass = loadClass(className);
            if (toolClass == null
                    || !isConcreteClass(toolClass)
                    || !ToolBase.class.isAssignableFrom(toolClass)) {
                continue;
            }

            toolConfigs.add(toToolConfig(toolClass.asSubclass(ToolBase.class)));
        }

        toolConfigs.sort(Comparator.comparing(AiToolInfoConfigEntity::getToolKey));
        return toolConfigs;
    }

    private Class<?> loadClass(String className) {
        try {
            return ClassUtils.forName(className, applicationContext.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ex) {
            log.warn("Skip tool class {}, failed to load class", className, ex);
            return null;
        }
    }

    private boolean isConcreteClass(Class<?> toolClass) {
        int modifiers = toolClass.getModifiers();
        return !toolClass.isAnnotation()
                && !toolClass.isInterface()
                && !Modifier.isAbstract(modifiers);
    }

    private AiToolInfoConfigEntity toToolConfig(Class<? extends ToolBase> toolClass) {
        ToolBase tool = resolveToolBean(toolClass);
        Method method = resolveCallMethod(toolClass);
        permission permission = resolvePermission(toolClass, method);
        String signatureHash = buildSignatureHash(tool);

        return new AiToolInfoConfigEntity()
                .setPermissionCode(permission == null ? null : permission.value())
                .setReadOnly(tool.isReadOnly())
                .setConcurrency(tool.isConcurrencySafe())
                .setStateInjected(tool.isStateInjected())
                .setToolKey(buildToolKey(toolClass, method))
                .setToolName(tool.getName())
                .setToolNameExplain(tool.getName())
                .setDescription(tool.getDescription())
                .setToolType("JAVA_BEAN")
                .setBeanName(resolveBeanName(toolClass))
                .setClassName(toolClass.getName())
                .setMethodName(method.getName())
                .setInputSchema(toJson(tool.getParameters()))
                .setOutputSchema(tool.getOutputSchema() == null ? null : toJson(tool.getOutputSchema()))
                .setSignatureHash(signatureHash)
                .setRiskLevel(resolveRiskLevel(tool))
                .setEnabled(true);
    }

    private ToolBase resolveToolBean(Class<? extends ToolBase> toolClass) {
        String[] beanNames = applicationContext.getBeanNamesForType(toolClass, false, false);
        try {
            if (beanNames.length > 0) {
                return applicationContext.getBean(beanNames[0], toolClass);
            }
            return applicationContext.getAutowireCapableBeanFactory().createBean(toolClass);
        } catch (BeansException ex) {
            throw new IllegalStateException("Failed to create tool bean: " + toolClass.getName(), ex);
        }
    }

    private String resolveBeanName(Class<?> toolClass) {
        String[] beanNames = applicationContext.getBeanNamesForType(toolClass, false, false);
        if (beanNames.length > 0) {
            return beanNames[0];
        }
        return Introspector.decapitalize(toolClass.getSimpleName());
    }

    private Method resolveCallMethod(Class<? extends ToolBase> toolClass) {
        try {
            return toolClass.getMethod("callAsync", ToolCallParam.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("ToolBase subclass must expose callAsync(ToolCallParam): "
                    + toolClass.getName(), ex);
        }
    }

    private String buildToolKey(Class<?> toolClass, Method method) {
        return toolClass.getName() + "#" + method.getName() + "(" + ToolCallParam.class.getName() + ")";
    }

    private permission resolvePermission(Class<?> toolClass, Method method) {
        permission methodPermission = method.getAnnotation(permission.class);
        if (methodPermission != null) {
            return methodPermission;
        }
        return toolClass.getAnnotation(permission.class);
    }

    private String buildSignatureHash(ToolBase tool) {
        Map<String, Object> signatureFields = new LinkedHashMap<>();
        signatureFields.put("name", tool.getName());
        signatureFields.put("description", tool.getDescription());
        signatureFields.put("readOnly", tool.isReadOnly());
        signatureFields.put("concurrencySafe", tool.isConcurrencySafe());
        signatureFields.put("stateInjected", tool.isStateInjected());
        signatureFields.put("dangerousFiles", dangerousPaths(tool, "dangerousFiles"));
        signatureFields.put("dangerousDirectories", dangerousPaths(tool, "dangerousDirectories"));
        signatureFields.put("converter", "");
        return DigestUtil.sha256Hex(toJson(signatureFields));
    }

    private String resolveRiskLevel(ToolBase tool) {
        List<String> dangerousFiles = dangerousPaths(tool, "dangerousFiles");
        List<String> dangerousDirectories = dangerousPaths(tool, "dangerousDirectories");
        if (tool.isReadOnly() && dangerousFiles.isEmpty() && dangerousDirectories.isEmpty()) {
            return "LOW";
        }
        if (!dangerousFiles.isEmpty() || !dangerousDirectories.isEmpty()) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    @SuppressWarnings("unchecked")
    private List<String> dangerousPaths(ToolBase tool, String fieldName) {
        try {
            Field field = ToolBase.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(tool);
            if (value instanceof List<?> list) {
                return (List<String>) list;
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            log.warn("Failed to read ToolBase field {}", fieldName, ex);
        }
        return Collections.emptyList();
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tool metadata", ex);
        }
    }
}
