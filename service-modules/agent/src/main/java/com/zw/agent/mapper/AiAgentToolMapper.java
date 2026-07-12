package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentToolEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zw.agent.entity.DTO.AgentBindToolDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * Agent与Tool绑定表：定义某个Agent启用了哪些工具 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-12
 */
@Mapper
public interface AiAgentToolMapper extends BaseMapper<AiAgentToolEntity> {

    List<AgentBindToolDTO> agentBindTools(Long agentId);
}
