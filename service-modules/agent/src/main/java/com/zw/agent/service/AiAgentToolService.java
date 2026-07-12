package com.zw.agent.service;

import com.zw.agent.entity.AiAgentToolEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.DTO.AgentBindToolDTO;

import java.util.List;

/**
 * <p>
 * Agent与Tool绑定表：定义某个Agent启用了哪些工具 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-12
 */
public interface AiAgentToolService extends IService<AiAgentToolEntity> {

    List<AgentBindToolDTO> agentBindTools(Long agentId);
}
