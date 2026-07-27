package com.zw.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zw.agent.entity.AiHttpHeaderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * HTTP请求头配置表 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-26
 */
@Mapper
public interface AiHttpHeaderMapper extends BaseMapper<AiHttpHeaderEntity> {

    @InterceptorIgnore(tenantLine = "true")
    List<AiHttpHeaderEntity> getHeaderlist(Long sourceId, Long tenantId);
}
