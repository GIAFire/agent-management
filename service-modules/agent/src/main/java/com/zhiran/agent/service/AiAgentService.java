package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiAgentEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.common.context.UserInfo;

import java.util.List;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
public interface AiAgentService extends IService<AiAgentEntity> {

    AgentConfigDTO getAgentConfigById(Long agentId, UserInfo userInfo);

    AgentConfigDTO getAgentConfigById(Long agentId, Long agentConfigId, UserInfo userInfo);

    List<AiAgentEntity> subAgentList(Long agentId, Long tenantId);
}
