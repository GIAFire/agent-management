package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiToolRolePermissionEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Agent权限规则表：定义某个工具在不同调用模式下允许、拒绝或询问 服务类
 * </p>
 *
 * @author zhiRan
 * @since 2026-06-28
 */
public interface AiToolRolePermissionService extends IService<AiToolRolePermissionEntity> {

    List<Map<String, String>> getToolPermissionByUserId(Long userId);

    List<AiToolRolePermissionEntity> permissionListByRoleId(String toolName, List<String> roleCodes,Long tenantId);
}
