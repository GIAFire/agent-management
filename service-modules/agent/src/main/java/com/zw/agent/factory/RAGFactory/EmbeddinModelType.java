package com.zw.agent.factory.RAGFactory;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.zw.agent.factory.modelFactory.ModelType;

public enum EmbeddinModelType implements IEnum<String> {
    SIMPLE("simple", "自定义向量模型"),
    BAILIAN("Bailian", "百炼知识库"),
    RAGFLOW("RAGFlow", "RAGFlow"),
    DIFY("Dify", "Dify");

    private final String code;
    private final String desc;

    EmbeddinModelType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    // 根据code获取枚举
    public static EmbeddinModelType fromCode(String code) {
        for (EmbeddinModelType type : values()) {
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
