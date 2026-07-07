package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.mapper.AiAgentWorkspaceFileMapper;
import com.zw.agent.runtime.AgentCallContext;
import com.zw.agent.service.AiAgentWorkspaceFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
        Path workspaceRoot = Paths.get(agentCallContext.getAgentConfig().getWorkspacePath())
                .toAbsolutePath()
                .normalize();

        String baseName = StringUtils.stripFilenameExtension(fileName); // "报告"
        String extension = StringUtils.getFilenameExtension(fileName);  // "md"

        String relativePath = String.format(
                "tenant_%d-user_%d/generateFiles/%s/%s",
                userInfo.getTenantId(),
                userInfo.getUserId(),
                runtimeContext.getSessionId(),
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
        entity.setSessionId(runtimeContext.getSessionId());
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
