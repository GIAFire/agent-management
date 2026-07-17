package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * Agent 消息表：保存用户输入、Agent 回复、工具消息等完整上下文 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiAgentMessageMapper extends BaseMapper<AiAgentMessageEntity> {

    @Select("""
            SELECT COALESCE(MAX(seq), 0)
            FROM ai_agent_message
            WHERE tenant_id = #{tenantId}
              AND session_id = #{sessionId}
              AND (deleted = 0 OR deleted IS NULL)
            """)
    Long selectMaxSeq(@Param("tenantId") Long tenantId, @Param("sessionId") Long sessionId);
}
