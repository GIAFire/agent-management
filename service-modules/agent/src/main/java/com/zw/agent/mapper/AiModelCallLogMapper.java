package com.zw.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zw.agent.entity.AiModelCallLogEntity;
import com.zw.agent.entity.DTO.ModelCallSummary;
import com.zw.agent.entity.DTO.ModelTrendRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiModelCallLogMapper extends BaseMapper<AiModelCallLogEntity> {

    @Select("""
            SELECT
                COUNT(*) AS totalCalls,
                COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS successCalls,
                COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedCalls,
                AVG(CASE WHEN status IN ('SUCCESS', 'FAILED') THEN duration_ms END) AS averageDurationMs
            FROM ai_model_call_log
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND call_source = 'AGENT_RUN'
              AND started_at >= #{start}
              AND started_at < #{end}
            """)
    @InterceptorIgnore(tenantLine = "true")
    ModelCallSummary selectBusinessSummary(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Select("""
            SELECT
                DATE(started_at) AS callDate,
                COUNT(*) AS callCount
            FROM ai_model_call_log
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND call_source = 'AGENT_RUN'
              AND started_at >= #{start}
              AND started_at < #{end}
            GROUP BY DATE(started_at)
            ORDER BY DATE(started_at)
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<ModelTrendRow> selectBusinessTrend(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Select("""
            <script>
            SELECT log.*
            FROM ai_model_call_log log
            INNER JOIN (
                SELECT model_config_id, MAX(id) AS latest_id
                FROM ai_model_call_log
                WHERE tenant_id = #{tenantId}
                  AND deleted = 0
                  AND call_source = 'MANUAL_TEST'
                  AND model_config_id IN
                  <foreach collection="modelIds" item="modelId" open="(" separator="," close=")">
                      #{modelId}
                  </foreach>
                GROUP BY model_config_id
            ) latest ON latest.latest_id = log.id
            WHERE log.tenant_id = #{tenantId}
              AND log.deleted = 0
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<AiModelCallLogEntity> selectLatestManualTests(
            @Param("tenantId") Long tenantId,
            @Param("modelIds") List<Long> modelIds
    );

    @Select("""
            SELECT COALESCE(SUM(total_tokens), 0)
            FROM ai_model_call_log
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND call_source = 'AGENT_RUN'
              AND started_at >= #{start}
              AND started_at < #{end}
            """)
    @InterceptorIgnore(tenantLine = "true")
    Long sumAgentRunTokens(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
