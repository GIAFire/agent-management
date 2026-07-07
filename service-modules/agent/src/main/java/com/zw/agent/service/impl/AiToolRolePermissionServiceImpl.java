package com.zw.agent.service.impl;

import com.zw.agent.entity.AiToolRolePermissionEntity;
import com.zw.agent.mapper.AiToolRolePermissionMapper;
import com.zw.agent.service.AiToolRolePermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Agent权限规则表：定义某个工具在不同调用模式下允许、拒绝或询问 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@RequiredArgsConstructor
@Service
public class AiToolRolePermissionServiceImpl extends ServiceImpl<AiToolRolePermissionMapper, AiToolRolePermissionEntity> implements AiToolRolePermissionService {

    private final AiToolRolePermissionMapper toolRolePermissionMapper;
    @Override
    public List<Map<String, String>> getToolPermissionByUserId(Long userId) {
        return toolRolePermissionMapper.getToolPermissionByUserId(userId);
    }
}
