package com.zw.agent.service;

import com.zw.agent.entity.AiSubagentEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
public interface AiSubagentService extends IService<AiSubagentEntity> {

    List<AiSubagentEntity> subAgentList(Long agentId);
}
