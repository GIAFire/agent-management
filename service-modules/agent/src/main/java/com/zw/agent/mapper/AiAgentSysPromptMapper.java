package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentSysPromptEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zw.agent.entity.DTO.SysPromptBindingCountRow;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-12
 */
@Mapper
public interface AiAgentSysPromptMapper extends BaseMapper<AiAgentSysPromptEntity> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            <script>
            SELECT prompt.id,
                   prompt.prompt_name AS promptName,
                   COUNT(DISTINCT agent.id) AS bindingCount
            FROM ai_agent_sys_prompt prompt
            LEFT JOIN ai_agent_config config
                   ON config.sys_prompt_id = prompt.id
                  AND config.tenant_id = #{tenantId}
                  AND config.deleted = 0
            LEFT JOIN ai_agent agent
                   ON agent.id = config.agent_id
                  AND agent.tenant_id = #{tenantId}
                  AND agent.deleted = 0
            WHERE prompt.tenant_id = #{tenantId}
              AND prompt.deleted = 0
              AND prompt.id IN
              <foreach collection="promptIds" item="promptId" open="(" separator="," close=")">
                  #{promptId}
              </foreach>
            GROUP BY prompt.id, prompt.prompt_name
            </script>
            """)
    List<SysPromptBindingCountRow> selectBindingCounts(
            @Param("tenantId") Long tenantId,
            @Param("promptIds") Collection<Long> promptIds
    );

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT prompt.id,
                   prompt.prompt_name AS promptName,
                   COUNT(DISTINCT agent.id) AS bindingCount
            FROM ai_agent_sys_prompt prompt
            LEFT JOIN ai_agent_config config
                   ON config.sys_prompt_id = prompt.id
                  AND config.tenant_id = #{tenantId}
                  AND config.deleted = 0
            LEFT JOIN ai_agent agent
                   ON agent.id = config.agent_id
                  AND agent.tenant_id = #{tenantId}
                  AND agent.deleted = 0
            WHERE prompt.tenant_id = #{tenantId}
              AND prompt.deleted = 0
            GROUP BY prompt.id, prompt.prompt_name, prompt.updated_at
            ORDER BY bindingCount DESC, prompt.updated_at DESC, prompt.id DESC
            LIMIT #{limit}
            """)
    List<SysPromptBindingCountRow> selectTopBindings(
            @Param("tenantId") Long tenantId,
            @Param("limit") int limit
    );

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT COUNT(DISTINCT prompt.id)
            FROM ai_agent_sys_prompt prompt
            INNER JOIN ai_agent_config config
                    ON config.sys_prompt_id = prompt.id
                   AND config.tenant_id = #{tenantId}
                   AND config.deleted = 0
            INNER JOIN ai_agent agent
                    ON agent.id = config.agent_id
                   AND agent.tenant_id = #{tenantId}
                   AND agent.deleted = 0
            WHERE prompt.tenant_id = #{tenantId}
              AND prompt.deleted = 0
            """)
    long countBoundPrompts(@Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT COUNT(DISTINCT agent.id)
            FROM ai_agent_sys_prompt prompt
            INNER JOIN ai_agent_config config
                    ON config.sys_prompt_id = prompt.id
                   AND config.tenant_id = #{tenantId}
                   AND config.deleted = 0
            INNER JOIN ai_agent agent
                    ON agent.id = config.agent_id
                   AND agent.tenant_id = #{tenantId}
                   AND agent.deleted = 0
            WHERE prompt.tenant_id = #{tenantId}
              AND prompt.deleted = 0
            """)
    long countBoundAgents(@Param("tenantId") Long tenantId);
}
