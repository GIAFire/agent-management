package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentSessionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent 会话表：保存用户与 Agent 的一次连续对话 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiAgentSessionMapper extends BaseMapper<AiAgentSessionEntity> {

}
