package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.mapper.AiAgentWorkspaceFileMapper;
import com.zw.agent.runtime.AgentCallContext;
import com.zw.agent.service.AiAgentWorkspaceFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import io.agentscope.core.agent.RuntimeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    private static final String SKILL_PACKAGE_ROOT = "skill-packages";
    private static final String SKILL_SOURCE_TYPE = "SKILL";
    private static final String SKILL_VISIBILITY = "TENANT";
    private static final Long SKILL_PACKAGE_AGENT_ID = 0L;
    private static final Long SKILL_PACKAGE_AGENT_CONFIG_ID = 0L;

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
                "%s/generateFiles/%s/%s",
                agentCallContext.getRuntimeUserKey(),
                agentCallContext.getRuntimeSessionKey(),
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

    @Override
    public AiAgentWorkspaceFileEntity saveSkillPackageFile(
            AiSkillInfoEntity skill,
            String relativeSkillPath,
            String fileName,
            String content
    ) {
        AiAgentWorkspaceFileEntity entity = new AiAgentWorkspaceFileEntity();
        fillSkillPackageFile(entity, skill, relativeSkillPath, fileName, content);
        workspaceFileMapper.insert(EntityDefaults.create(entity));
        return entity;
    }

    @Override
    public AiAgentWorkspaceFileEntity updateSkillPackageFile(
            AiAgentWorkspaceFileEntity workspaceFile,
            AiSkillInfoEntity skill,
            String relativeSkillPath,
            String fileName,
            String content
    ) {
        AiAgentWorkspaceFileEntity entity = workspaceFile == null ? new AiAgentWorkspaceFileEntity() : workspaceFile;
        fillSkillPackageFile(entity, skill, relativeSkillPath, fileName, content);
        if (entity.getId() == null) {
            workspaceFileMapper.insert(EntityDefaults.create(entity));
            return entity;
        }
        workspaceFileMapper.updateById(EntityDefaults.update(entity));
        return entity;
    }

    private void fillSkillPackageFile(
            AiAgentWorkspaceFileEntity entity,
            AiSkillInfoEntity skill,
            String relativeSkillPath,
            String fileName,
            String content
    ) {
        UserInfo userInfo = UserContext.get();
        Long tenantId = userInfo != null ? userInfo.getTenantId() : skill.getTenantId();
        Long userId = userInfo != null ? userInfo.getUserId() : skill.getCreatedBy();
        String normalizedContent = content == null ? "" : content;
        byte[] bytes = normalizedContent.getBytes(StandardCharsets.UTF_8);
        String extension = StringUtils.getFilenameExtension(fileName);
        String title = StringUtils.stripFilenameExtension(fileName);
        String storagePath = buildSkillStoragePath(tenantId, skill.getSkillKey(), relativeSkillPath, fileName);

        writeLocalFile(storagePath, normalizedContent);

        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setAgentId(SKILL_PACKAGE_AGENT_ID);
        entity.setAgentConfigId(SKILL_PACKAGE_AGENT_CONFIG_ID);
        entity.setSessionId(null);
        entity.setRunId(null);
        entity.setToolCallId(null);
        entity.setFileName(fileName);
        entity.setRelativePath(storagePath);
        entity.setStorageBackend("LOCAL");
        entity.setStorageKey(storagePath);
        entity.setMimeType(resolveMimeType(extension));
        entity.setFileExt(extension);
        entity.setSizeBytes((long) bytes.length);
        entity.setChecksum(sha256(bytes));
        entity.setTitle(StringUtils.hasText(title) ? title : fileName);
        entity.setSummary(skill.getDescription());
        entity.setSourceType(SKILL_SOURCE_TYPE);
        entity.setVisibility(SKILL_VISIBILITY);
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(userId);
        }
    }

    private String buildSkillStoragePath(Long tenantId, String skillKey, String relativeSkillPath, String fileName) {
        String tenantSegment = tenantId == null ? "tenant_default" : "tenant_" + tenantId;
        String skillSegment = sanitizePathSegment(skillKey);
        String filePath = StringUtils.hasText(relativeSkillPath) ? relativeSkillPath : fileName;
        filePath = filePath.replace("\\", "/").replaceAll("^/+", "");
        return String.join("/", SKILL_PACKAGE_ROOT, tenantSegment, skillSegment, filePath);
    }

    private String sanitizePathSegment(String value) {
        String text = value == null ? "skill" : value.trim();
        text = text.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
        text = text.replaceAll("[^a-zA-Z0-9._-]+", "-");
        text = text.replaceAll("^-+|-+$", "");
        return StringUtils.hasText(text) ? text : "skill";
    }

    private void writeLocalFile(String relativePath, String content) {
        Path projectRoot = Paths.get("").toAbsolutePath().normalize();
        Path targetPath = projectRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(projectRoot)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("写入技能包文件失败", e);
        }
    }

    private String resolveMimeType(String extension) {
        if ("md".equalsIgnoreCase(extension) || "markdown".equalsIgnoreCase(extension)) {
            return "text/markdown;charset=UTF-8";
        }
        if ("json".equalsIgnoreCase(extension)) {
            return "application/json;charset=UTF-8";
        }
        if ("txt".equalsIgnoreCase(extension)) {
            return "text/plain;charset=UTF-8";
        }
        return "application/octet-stream";
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
