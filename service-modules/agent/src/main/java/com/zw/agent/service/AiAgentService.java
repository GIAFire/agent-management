package com.zw.agent.service;

import com.zw.agent.entity.AiAgentEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.common.context.UserInfo;

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

    List<AgentConfigDTO> getAgentInfoList();

    Boolean createAgent(AgentConfigDTO agentVO);

    List<AiAgentEntity> subAgentList(Long agentId);
}
