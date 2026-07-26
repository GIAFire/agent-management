package com.zw.agent.factory.stateStoreFactory;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum StateStoreType implements IEnum<String> {
    LOCAL_FILE("local_file", "本地文件"),
    REDIS("redis", "Redis"),
    MYSQL("mysql", "MySQL");

    private final String code;
    private final String desc;

    StateStoreType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    @JsonCreator
    public static StateStoreType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim().toLowerCase(Locale.ROOT)) {
            case "local_file", "local-file", "localfile" -> LOCAL_FILE;
            case "redis" -> REDIS;
            case "mysql" -> MYSQL;
            default -> throw new IllegalArgumentException("不支持的会话状态存储类型: " + code);
        };
    }

    @Override
    @JsonValue
    public String getValue() {
        return code;
    }
}
