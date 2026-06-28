package com.zw.agent.tools.applicationRunner;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.service.AiToolInfoConfigService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ToolRegistrySyncRunner implements ApplicationRunner {

    private static final String TOOL_BASE_PACKAGE = "com.zw.agent.tools";
    private static final Logger log = LoggerFactory.getLogger(ToolRegistrySyncRunner.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ApplicationContext applicationContext;
    private final AiToolInfoConfigService toolInfoConfigService;


    @Override
    public void run(ApplicationArguments args) {
        // 扫描所有com.zw.agent.tools包下的类
        List<AiToolInfoConfigEntity> toolConfigs = scanToolConfigs();

        if (toolConfigs.isEmpty()) {
            log.info("No @Tool methods found under package {}", TOOL_BASE_PACKAGE);
            return;
        }
        // 使数用批量upsert语句: 将这些信息存入据库
        int count = toolInfoConfigService.upsertBatch(toolConfigs);
        log.info("Synced {} tool definitions into ai_tool_info_config, affected rows={}", toolConfigs.size(), count);

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
            if (toolClass == null || !isConcreteClass(toolClass)) {
                continue;
            }

            for (Method method : toolClass.getDeclaredMethods()) {
                Tool tool = method.getAnnotation(Tool.class);
                if (tool == null || method.isBridge() || method.isSynthetic()) {
                    continue;
                }
                toolConfigs.add(toToolConfig(toolClass, method, tool));
            }
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

    private AiToolInfoConfigEntity toToolConfig(Class<?> toolClass, Method method, Tool tool) {
        permission permission = method.getAnnotation(permission.class);
        String signatureHash = buildSignatureHash(tool);

        return new AiToolInfoConfigEntity()
                .setPermissionCode(permission == null ? null : permission.value())
                .setReadOnly(tool.readOnly())
                .setConcurrency(tool.concurrencySafe())
                .setStateInjected(tool.stateInjected())
                .setToolKey(buildToolKey(toolClass, method))
                .setToolName(tool.name())
                .setToolNameExplain(tool.name())
                .setDescription(tool.description())
                .setToolType("JAVA_BEAN")
                .setBeanName(resolveBeanName(toolClass))
                .setClassName(toolClass.getName())
                .setMethodName(method.getName())
                .setInputSchema(toJson(buildInputMetadata(toolClass, method, tool)))
                .setOutputSchema(toJson(buildOutputMetadata(method)))
                .setSignatureHash(signatureHash)
                .setRiskLevel(resolveRiskLevel(tool))
                .setEnabled(true);
    }

    private String resolveBeanName(Class<?> toolClass) {
        String[] beanNames = applicationContext.getBeanNamesForType(toolClass, false, false);
        if (beanNames.length > 0) {
            return beanNames[0];
        }
        return Introspector.decapitalize(toolClass.getSimpleName());
    }

    private String buildToolKey(Class<?> toolClass, Method method) {
        String parameterTypes = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.joining(","));
        return toolClass.getName() + "#" + method.getName() + "(" + parameterTypes + ")";
    }

    private String buildSignatureHash(Tool tool) {
        Map<String, Object> signatureFields = new LinkedHashMap<>();
        signatureFields.put("name", tool.name());
        signatureFields.put("description", tool.description());
        signatureFields.put("readOnly", tool.readOnly());
        signatureFields.put("concurrencySafe", tool.concurrencySafe());
        signatureFields.put("stateInjected", tool.stateInjected());
        signatureFields.put("dangerousFiles", Arrays.asList(tool.dangerousFiles()));
        signatureFields.put("dangerousDirectories", Arrays.asList(tool.dangerousDirectories()));
        signatureFields.put("converter", tool.converter().getName());
        return DigestUtil.sha256Hex(toJson(signatureFields));
    }

    private Map<String, Object> buildInputMetadata(Class<?> toolClass, Method method, Tool tool) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("toolName", tool.name());
        schema.put("className", toolClass.getName());
        schema.put("methodName", method.getName());
        schema.put("parameterTypes", Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.toList()));

        List<Map<String, Object>> parameters = new ArrayList<>();
        Parameter[] methodParameters = method.getParameters();
        for (int i = 0; i < methodParameters.length; i++) {
            Parameter parameter = methodParameters[i];
            ToolParam toolParam = parameter.getAnnotation(ToolParam.class);

            Map<String, Object> parameterMetadata = new LinkedHashMap<>();
            parameterMetadata.put("index", i);
            parameterMetadata.put("name", resolveParameterName(parameter, toolParam));
            parameterMetadata.put("javaName", parameter.getName());
            parameterMetadata.put("type", parameter.getType().getName());
            parameterMetadata.put("genericType", parameter.getParameterizedType().getTypeName());
            parameterMetadata.put("required", toolParam != null && toolParam.required());
            parameterMetadata.put("description", toolParam == null ? "" : toolParam.description());
            parameterMetadata.put("modelVisible", toolParam != null);
            parameters.add(parameterMetadata);
        }
        schema.put("parameters", parameters);
        return schema;
    }

    private String resolveParameterName(Parameter parameter, ToolParam toolParam) {
        if (toolParam != null && StringUtils.hasText(toolParam.name())) {
            return toolParam.name();
        }
        return parameter.getName();
    }

    private Map<String, Object> buildOutputMetadata(Method method) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", method.getReturnType().getName());
        schema.put("genericType", method.getGenericReturnType().getTypeName());
        return schema;
    }

    private String resolveRiskLevel(Tool tool) {
        if (tool.readOnly() && tool.dangerousFiles().length == 0 && tool.dangerousDirectories().length == 0) {
            return "LOW";
        }
        if (tool.dangerousFiles().length > 0 || tool.dangerousDirectories().length > 0) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tool metadata", ex);
        }
    }
}
