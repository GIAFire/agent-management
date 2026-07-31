package com.zw.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zw.agent.entity.AiAgentRunLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zw.agent.entity.DTO.AgentRunSummary;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * Agent 运行表：一次用户请求对应一次 AgentScope call 或 streamEvents 执行 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiAgentRunLogMapper extends BaseMapper<AiAgentRunLogEntity> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT
                #{agentId} AS agentId,
                COUNT(*) AS totalRuns,
                COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS successRuns,
                COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedRuns,
                COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelledRuns,
                AVG(CASE
                    WHEN status IN ('SUCCESS', 'FAILED', 'CANCELLED') AND ended_at IS NOT NULL
                    THEN TIMESTAMPDIFF(MICROSECOND, started_at, ended_at) / 1000.0
                END) AS averageDurationMs
            FROM ai_agent_run_log
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND started_at >= #{start}
              AND started_at < #{end}
              AND (#{agentId} IS NULL OR agent_id = #{agentId})
            """)
    AgentRunSummary selectSummary(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("agentId") Long agentId
    );

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "<script>",
            "SELECT",
            "agent_id AS agentId,",
            "COUNT(*) AS totalRuns,",
            "COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS successRuns,",
            "COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedRuns,",
            "COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelledRuns,",
            "AVG(CASE WHEN status IN ('SUCCESS', 'FAILED', 'CANCELLED') AND ended_at IS NOT NULL",
            "THEN TIMESTAMPDIFF(MICROSECOND, started_at, ended_at) / 1000.0 END) AS averageDurationMs",
            "FROM ai_agent_run_log",
            "WHERE tenant_id = #{tenantId}",
            "AND deleted = 0",
            "AND started_at &gt;= #{start}",
            "AND started_at &lt; #{end}",
            "AND agent_id IN",
            "<foreach collection='agentIds' item='agentId' open='(' separator=',' close=')'>",
            "#{agentId}",
            "</foreach>",
            "GROUP BY agent_id",
            "</script>"
    })
    List<AgentRunSummary> selectAgentSummaries(
            @Param("tenantId") Long tenantId,
            @Param("agentIds") Collection<Long> agentIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
