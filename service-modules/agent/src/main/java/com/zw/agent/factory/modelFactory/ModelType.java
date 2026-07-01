package com.zw.agent.factory.modelFactory;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ModelType implements IEnum<String> {
    OPENAI("openai", "OpenAI模型"),
    DASH_SCOPE("dashscope", "通义千问"),
    OLLAMA("ollama", "Ollama本地模型"),
    ANTHROPIC("anthropic", "Anthropic模型");

    private final String code;
    private final String desc;

    ModelType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    // 根据code获取枚举
    public static ModelType fromCode(String code) {
        for (ModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String getValue() {
        return code;
    }
}
