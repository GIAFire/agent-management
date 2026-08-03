package com.zhiran.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zhiran.agent.entity.DTO.RunOverviewQuickAgentRow;
import com.zhiran.agent.entity.DTO.RunOverviewRecentInteractionRow;
import com.zhiran.agent.entity.DTO.RunOverviewTrendRow;
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
                run.id AS runId,
                run.session_id AS sessionId,
                run.agent_id AS agentId,
                agent.agent_code AS agentCode,
                agent.agent_name AS agentName,
                input_message.text_content AS userMessage,
                output_message.text_content AS assistantMessage,
                run.status AS status,
                run.started_at AS startedAt
            FROM ai_agent_run_log run
            INNER JOIN ai_agent_session agent_session
              ON agent_session.id = run.session_id
             AND agent_session.tenant_id = run.tenant_id
             AND agent_session.user_id = #{userId}
             AND (agent_session.deleted = 0 OR agent_session.deleted IS NULL)
            INNER JOIN ai_agent_message input_message
              ON input_message.id = run.input_message_id
             AND input_message.tenant_id = run.tenant_id
             AND input_message.session_id = run.session_id
             AND input_message.role = 'USER'
             AND input_message.message_type = 'USER_TEXT'
             AND (input_message.deleted = 0 OR input_message.deleted IS NULL)
            LEFT JOIN ai_agent_message output_message
              ON output_message.id = run.output_message_id
             AND output_message.tenant_id = run.tenant_id
             AND output_message.session_id = run.session_id
             AND output_message.role = 'ASSISTANT'
             AND output_message.message_type = 'ASSISTANT_TEXT'
             AND (output_message.deleted = 0 OR output_message.deleted IS NULL)
            LEFT JOIN ai_agent agent
              ON agent.id = run.agent_id
             AND agent.tenant_id = run.tenant_id
             AND agent.deleted = 0
            WHERE run.tenant_id = #{tenantId}
              AND run.deleted = 0
            ORDER BY run.started_at DESC, run.id DESC
            LIMIT #{limit}
            """)
    List<RunOverviewRecentInteractionRow> selectRecentInteractions(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM ai_agent_run_log run",
            "INNER JOIN ai_agent_session agent_session",
            "ON agent_session.id = run.session_id",
            "AND agent_session.tenant_id = run.tenant_id",
            "AND agent_session.user_id = #{userId}",
            "AND (agent_session.deleted = 0 OR agent_session.deleted IS NULL)",
            "INNER JOIN ai_agent_message input_message",
            "ON input_message.id = run.input_message_id",
            "AND input_message.tenant_id = run.tenant_id",
            "AND input_message.session_id = run.session_id",
            "AND input_message.role = 'USER'",
            "AND input_message.message_type = 'USER_TEXT'",
            "AND (input_message.deleted = 0 OR input_message.deleted IS NULL)",
            "LEFT JOIN ai_agent_message output_message",
            "ON output_message.id = run.output_message_id",
            "AND output_message.tenant_id = run.tenant_id",
            "AND output_message.session_id = run.session_id",
            "AND output_message.role = 'ASSISTANT'",
            "AND output_message.message_type = 'ASSISTANT_TEXT'",
            "AND (output_message.deleted = 0 OR output_message.deleted IS NULL)",
            "LEFT JOIN ai_agent agent",
            "ON agent.id = run.agent_id",
            "AND agent.tenant_id = run.tenant_id",
            "AND agent.deleted = 0",
            "WHERE run.tenant_id = #{tenantId}",
            "AND run.deleted = 0",
            "<if test=\"keyword != null and keyword != ''\">",
            "AND (input_message.text_content LIKE CONCAT('%', #{keyword}, '%')",
            "OR output_message.text_content LIKE CONCAT('%', #{keyword}, '%')",
            "OR agent.agent_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "</script>"
    })
    long countRecentInteractions(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("keyword") String keyword
    );

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "<script>",
            "SELECT",
            "run.id AS runId,",
            "run.session_id AS sessionId,",
            "run.agent_id AS agentId,",
            "agent.agent_code AS agentCode,",
            "agent.agent_name AS agentName,",
            "input_message.text_content AS userMessage,",
            "output_message.text_content AS assistantMessage,",
            "run.status AS status,",
            "run.started_at AS startedAt",
            "FROM ai_agent_run_log run",
            "INNER JOIN ai_agent_session agent_session",
            "ON agent_session.id = run.session_id",
            "AND agent_session.tenant_id = run.tenant_id",
            "AND agent_session.user_id = #{userId}",
            "AND (agent_session.deleted = 0 OR agent_session.deleted IS NULL)",
            "INNER JOIN ai_agent_message input_message",
            "ON input_message.id = run.input_message_id",
            "AND input_message.tenant_id = run.tenant_id",
            "AND input_message.session_id = run.session_id",
            "AND input_message.role = 'USER'",
            "AND input_message.message_type = 'USER_TEXT'",
            "AND (input_message.deleted = 0 OR input_message.deleted IS NULL)",
            "LEFT JOIN ai_agent_message output_message",
            "ON output_message.id = run.output_message_id",
            "AND output_message.tenant_id = run.tenant_id",
            "AND output_message.session_id = run.session_id",
            "AND output_message.role = 'ASSISTANT'",
            "AND output_message.message_type = 'ASSISTANT_TEXT'",
            "AND (output_message.deleted = 0 OR output_message.deleted IS NULL)",
            "LEFT JOIN ai_agent agent",
            "ON agent.id = run.agent_id",
            "AND agent.tenant_id = run.tenant_id",
            "AND agent.deleted = 0",
            "WHERE run.tenant_id = #{tenantId}",
            "AND run.deleted = 0",
            "<if test=\"keyword != null and keyword != ''\">",
            "AND (input_message.text_content LIKE CONCAT('%', #{keyword}, '%')",
            "OR output_message.text_content LIKE CONCAT('%', #{keyword}, '%')",
            "OR agent.agent_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY run.started_at DESC, run.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<RunOverviewRecentInteractionRow> selectRecentInteractionsPage(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
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
