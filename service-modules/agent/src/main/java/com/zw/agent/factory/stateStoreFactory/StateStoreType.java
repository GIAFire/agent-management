package com.zw.agent.factory.stateStoreFactory;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum StateStoreType implements IEnum<String> {
    LOCAL_FILE("local_file", "本地文件"),
    REDIS("redis", "Redis"),
    DATABASE("database", "数据库");

    private final String code;
    private final String desc;

    StateStoreType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    // 根据code获取枚举
    public static StateStoreType fromCode(String code) {
        for (StateStoreType type : values()) {
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
