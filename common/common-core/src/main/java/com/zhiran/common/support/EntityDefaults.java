package com.zhiran.common.support;

import com.zhiran.common.context.UserContext;
import com.zhiran.common.context.UserInfo;
import com.zhiran.common.entity.BaseEntity;

import java.time.LocalDateTime;

public final class EntityDefaults {

    private EntityDefaults() {
    }

    public static <T extends BaseEntity> T create(T entity) {
        LocalDateTime now = LocalDateTime.now();
        UserInfo user = UserContext.get();
        if (entity.getDeleted() == null) {
            entity.setDeleted(0);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        if (entity.getCreatedBy() == null && user != null) {
            entity.setCreatedBy(user.getUserId());
        }
        entity.setUpdatedAt(now);
        if (user != null) {
            entity.setUpdateBy(user.getUserId());
        }
        return entity;
    }

    public static <T extends BaseEntity> T update(T entity) {
        LocalDateTime now = LocalDateTime.now();
        UserInfo user = UserContext.get();
        if (entity.getDeleted() == null) {
            entity.setDeleted(0);
        }
        entity.setUpdatedAt(now);
        if (user != null) {
            entity.setUpdateBy(user.getUserId());
        }
        return entity;
    }
}

