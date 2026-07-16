package com.zw.agent.service;

import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
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

    AiAgentWorkspaceFileEntity saveSkillPackageFile(AiSkillInfoEntity skill, String relativeSkillPath, String fileName, String content);

    AiAgentWorkspaceFileEntity updateSkillPackageFile(AiAgentWorkspaceFileEntity workspaceFile, AiSkillInfoEntity skill, String relativeSkillPath, String fileName, String content);
}
