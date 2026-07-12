package com.zw.agent.factory.RAGFactory.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.zw.agent.factory.modelFactory.ModelType;

public enum ApiType implements IEnum<String> {

    OPENAI("openai", "OpenAI模型"),
    DASH_SCOPE("dashscope", "通义千问"),
    OLLAMA("ollama", "Ollama本地模型"),
    ZHIPU("zhipu", "智谱模型");

    private final String code;
    private final String desc;

    ApiType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    // 根据code获取枚举
    public static ApiType fromCode(String code) {
        for (ApiType type : values()) {
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
