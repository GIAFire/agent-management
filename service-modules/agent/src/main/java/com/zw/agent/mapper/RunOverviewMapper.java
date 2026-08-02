package com.zw.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zw.agent.entity.DTO.RunOverviewQuickAgentRow;
import com.zw.agent.entity.DTO.RunOverviewTrendRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RunOverviewMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT
                DATE(started_at) AS runDate,
                COUNT(*) AS runCount,
                COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END), 0)
                    AS successCount
            FROM ai_agent_run_log
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND started_at >= #{start}
              AND started_at < #{end}
            GROUP BY DATE(started_at)
            ORDER BY DATE(started_at)
            """)
    List<RunOverviewTrendRow> selectTrend(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT
                agent.id AS id,
                agent.agent_code AS agentCode,
                agent.agent_name AS agentName,
                agent.description AS description,
                model.id AS modelId,
                model.config_name AS modelConfigName,
                model.provider_name AS providerName,
                model.model_name AS modelName,
                COUNT(run.id) AS runCount30Days
            FROM ai_agent agent
            LEFT JOIN ai_agent_config config
              ON config.agent_id = agent.id
             AND config.tenant_id = agent.tenant_id
             AND config.deleted = 0
            LEFT JOIN ai_agent_model model
              ON model.id = config.model_id
             AND model.tenant_id = agent.tenant_id
             AND model.deleted = 0
             AND model.status = 1
            LEFT JOIN ai_agent_run_log run
              ON run.agent_id = agent.id
             AND run.tenant_id = agent.tenant_id
             AND run.deleted = 0
             AND run.started_at >= #{runStart}
             AND run.started_at < #{runEnd}
            WHERE agent.tenant_id = #{tenantId}
              AND agent.deleted = 0
            GROUP BY
                agent.id,
                agent.agent_code,
                agent.agent_name,
                agent.description,
                agent.updated_at,
                model.id,
                model.config_name,
                model.provider_name,
                model.model_name
            ORDER BY runCount30Days DESC, agent.updated_at DESC, agent.id DESC
            LIMIT #{limit}
            """)
    List<RunOverviewQuickAgentRow> selectQuickAgents(
            @Param("tenantId") Long tenantId,
            @Param("runStart") LocalDateTime runStart,
            @Param("runEnd") LocalDateTime runEnd,
            @Param("limit") int limit
    );
}
