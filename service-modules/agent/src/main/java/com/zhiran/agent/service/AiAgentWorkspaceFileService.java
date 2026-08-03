package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiAgentWorkspaceFileEntity;
import com.zhiran.agent.entity.AiSkillInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import io.agentscope.core.agent.RuntimeContext;

/**
 * <p>
 * Agent工作区文件表 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-05
 */
public interface AiAgentWorkspaceFileService extends IService<AiAgentWorkspaceFileEntity> {

    AiAgentWorkspaceFileEntity saveGeneratedFile(RuntimeContext runtimeContext, String toolCallId, String fileName, String content);

}
