package com.zw.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 租户工具授权配置表 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@Mapper
public interface AiToolTenantConfigMapper extends BaseMapper<AiToolInfoConfigEntity> {

    List<String> getToolByTenantBeans(Long tenantId);
}
