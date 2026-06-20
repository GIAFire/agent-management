package com.zw.agent.mapper;

import com.zw.agent.entity.SysTenantEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 租户表：平台多租户隔离的根表 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenantEntity> {

}
