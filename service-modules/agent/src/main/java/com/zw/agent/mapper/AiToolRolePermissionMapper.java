package com.zw.agent.mapper;

import com.zw.agent.entity.AiToolRolePermissionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Agent权限规则表：定义某个工具在不同调用模式下允许、拒绝或询问 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@Mapper
public interface AiToolRolePermissionMapper extends BaseMapper<AiToolRolePermissionEntity> {

    List<Map<String, String>> getToolPermissionByUserId(Long userId);
}
