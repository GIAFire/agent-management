package com.zw.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 全局工具配置表 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@Mapper
public interface AiToolInfoConfigMapper extends BaseMapper<AiToolInfoConfigEntity> {

    @InterceptorIgnore(tenantLine = "true")
    int upsertBatch(@Param("list") List<AiToolInfoConfigEntity> list);
}
