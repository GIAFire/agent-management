package com.zhiran.agent.service.impl;

import com.zhiran.agent.constant.AgentConstant;
import com.zhiran.agent.entity.AiAgentWorkspaceFileEntity;
import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.agent.mapper.AiAgentWorkspaceFileMapper;
import com.zhiran.agent.runtime.AgentCallContext;
import com.zhiran.agent.service.AiAgentWorkspaceFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiran.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>
 * Agent工作区文件表 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-05
 */
@RequiredArgsConstructor
@Service
public class AiAgentWorkspaceFileServiceImpl extends ServiceImpl<AiAgentWorkspaceFileMapper, AiAgentWorkspaceFileEntity> implements AiAgentWorkspaceFileService {

    private final AiAgentWorkspaceFileMapper workspaceFileMapper;

    @Override
    public AiAgentWorkspaceFileEntity saveGeneratedFile(
            RuntimeContext runtimeContext,
            String toolCallId,
            String fileName,
            String content
    ) {
        AgentCallContext agentCallContext = runtimeContext.get(AgentCallContext.class);
        AgentConfigDTO agentConfig = agentCallContext.getAgentConfig();
        UserInfo userInfo = agentCallContext.getUserInfo();

        Path baseWorkspace = Paths.get(AgentConstant.WORK_PACE_PATH);
        Path workspaceRoot = baseWorkspace
                .resolve("tenants").resolve(String.valueOf(agentConfig.getTenantId()))
                .resolve("users").resolve(String.valueOf(userInfo.getUserId()))
                .resolve("agents").resolve(String.valueOf(agentConfig.getAgentId()));

        String baseName = StringUtils.stripFilenameExtension(fileName); // "报告"
        String extension = StringUtils.getFilenameExtension(fileName);  // "md"

        String relativePath = String.format(
                "temp/%s/%s",
                agentCallContext.getSessionId(),
                fileName
        );

        Path targetPath = workspaceRoot.resolve(relativePath).normalize();

        if (!targetPath.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("写入工作区文件失败", e);
        }

        AiAgentWorkspaceFileEntity entity = new AiAgentWorkspaceFileEntity();
        entity.setTenantId(userInfo.getTenantId());
        entity.setUserId(userInfo.getUserId());
        entity.setAgentId(agentConfig.getAgentId());
        entity.setAgentConfigId(agentConfig.getAgentConfigId());
        entity.setSessionId(String.valueOf(agentCallContext.getSessionId()));
        entity.setRunId(agentCallContext.getRunId());
        entity.setToolCallId(toolCallId);
        entity.setFileName(fileName);
        entity.setFileExt(extension);
        entity.setRelativePath(relativePath);
        entity.setStorageBackend("LOCAL");
        entity.setStorageKey(relativePath);
        entity.setMimeType("application/octet-stream");
        entity.setSizeBytes((long) content.getBytes().length);
        entity.setTitle(baseName);
        entity.setSourceType("AGENT");
        entity.setVisibility("SESSION");
        entity.setCreatedBy(userInfo.getUserId());

        workspaceFileMapper.insert(entity);

        return entity;
    }
}
