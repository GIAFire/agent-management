package com.zw.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zw.agent.entity.AiHttpHeaderEntity;
import com.zw.agent.constant.enumeration.HeaderSourceType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

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
    List<AiHttpHeaderEntity> getHeaderList(
            @Param("sourceId") Long sourceId,
            @Param("tenantId") Long tenantId,
            @Param("source") HeaderSourceType source
    );

    @Delete("DELETE FROM ai_http_header WHERE source_id = #{sourceId} AND source = 'remoteSubAgent' AND tenant_id = #{tenantId}")
    int hardDeleteRemoteSubagentHeaders(@Param("sourceId") Long sourceId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM ai_http_header WHERE source_id = #{sourceId} AND source = 'model' AND tenant_id = #{tenantId}")
    int hardDeleteModelHeaders(@Param("sourceId") Long sourceId, @Param("tenantId") Long tenantId);
}
