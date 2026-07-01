package com.zw.common.support;

import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.entity.BaseEntity;

import java.time.LocalDateTime;

public final class EntityDefaults {

    private EntityDefaults() {
    }

    public static <T extends BaseEntity> T create(T entity) {
        LocalDateTime now = LocalDateTime.now();
        UserInfo user = UserContext.get();

        if (entity.getTenantId() == null) {
            entity.setTenantId(user != null && user.getTenantId() != null ? user.getTenantId() : 1L);
        }
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

        if (entity.getTenantId() == null && user != null && user.getTenantId() != null) {
            entity.setTenantId(user.getTenantId());
        }
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

