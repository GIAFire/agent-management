package com.zhiran.agent.constant.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum HeaderSourceType implements IEnum<String> {
    MODEL("model", "模型"),
    EMBEDDING("embedding", "向量模型"),
    REMOTE_SUB_AGENT("remoteSubAgent", "远程子Agent");

    private final String code;
    private final String desc;

    HeaderSourceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    // 根据code获取枚举
    public static HeaderSourceType fromCode(String code) {
        for (HeaderSourceType type : values()) {
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
