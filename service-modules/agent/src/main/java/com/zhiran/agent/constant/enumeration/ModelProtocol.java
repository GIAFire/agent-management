package com.zhiran.agent.constant.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;

public enum ModelProtocol implements IEnum<String> {
    OPENAI_COMPATIBLE("openaiCompatible", "OpenAI 兼容协议"),
    DASH_SCOPE("dashscope", "DashScope 协议"),
    ANTHROPIC("anthropic", "Anthropic 协议"),
    OLLAMA("ollama", "Ollama 协议"),
    DEEPSEEK("DeepSeek", "DeepSeek 协议"),
    GLM("GLM", "GLM 协议"),
    KIMI("Kimi", "Kimi 协议"),
    MINIMAX("MiniMax", "MiniMax 协议");

    private final String code;
    private final String description;

    ModelProtocol(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static ModelProtocol fromCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace("-", "")
                .replace("_", "")
                .toLowerCase(Locale.ROOT);
        if ("openai".equals(normalized) || "openaicompatible".equals(normalized)) {
            return OPENAI_COMPATIBLE;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.replace("_", "")
                        .toLowerCase(Locale.ROOT)
                        .equals(normalized)
                        || item.name().replace("_", "")
                        .toLowerCase(Locale.ROOT)
                        .equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported model protocol: " + value));
    }

    @Override
    public String getValue() {
        return code;
    }
}
