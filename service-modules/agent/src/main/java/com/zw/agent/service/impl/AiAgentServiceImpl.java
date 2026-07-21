package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zw.agent.entity.*;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.exception.AgentConfigException;
import com.zw.agent.mapper.*;
import com.zw.agent.service.AiAgentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.support.EntityDefaults;
import com.zw.common.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.zw.common.utils.DefaultValue.*;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RequiredArgsConstructor
@Service
public class AiAgentServiceImpl extends ServiceImpl<AiAgentMapper, AiAgentEntity> implements AiAgentService {


    private final AiAgentMapper aiAgentMapper;
    private final AiAgentConfigMapper agentConfigMapper;
    private final AiKnowledgeAgentBindingMapper knowledgeAgentBindingMapper;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiSubagentAgentBindingMapper subagentAgentBindingMapper;
    private final AiSubagentMapper subagentMapper;
    private final AiAgentToolMapper agentToolMapper;
    private final AiToolInfoConfigMapper toolInfoConfigMapper;

    public AgentConfigDTO getAgentConfigById(Long agentId) {
        if (agentId == null) {
            throw new AgentConfigException("agentId 不能为空");
        }
        AgentConfigDTO agentConfig = aiAgentMapper.getAgentConfigById(agentId);

        try {
            String decrypt = AESUtil.decrypt(agentConfig.getApiKey());
            agentConfig.setApiKey(decrypt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return agentConfig;
    }

    @Override
    public List<AgentConfigDTO> getAgentInfoList() {
        return aiAgentMapper.getAgentInfoList();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean createAgent(AgentConfigDTO agentVO) {
        if (agentVO == null) {
            throw new IllegalArgumentException("agent config must not be null");
        }
        if (!hasText(agentVO.getAgentCode())) {
            throw new IllegalArgumentException("agentCode must not be blank");
        }
        if (!hasText(agentVO.getAgentName())) {
            throw new IllegalArgumentException("agentName must not be blank");
        }
        if (agentVO.getModelId() == null) {
            throw new IllegalArgumentException("modelId must not be null");
        }

        AiAgentEntity agent = new AiAgentEntity()
                .setAgentCode(agentVO.getAgentCode())
                .setAgentName(agentVO.getAgentName())
                .setDescription(firstText(agentVO.getAgentDescription(), agentVO.getAgentName()))
                .setAgentType(firstText(agentVO.getAgentType(), "HARNESS"))
                .setStatus(defaultInt(agentVO.getAgentStatus(), 1));
        agent.setTenantId(defaultLong(agentVO.getTenantId(), 1L));
        aiAgentMapper.insert(EntityDefaults.create(agent));

        AiAgentConfigEntity config = new AiAgentConfigEntity()
                .setAgentId(agent.getId())
                .setVersionNo(firstText(agentVO.getVersionNo(), "v1"))
                .setSysPromptId(agentVO.getSysPromptId())
                .setModelId(agentVO.getModelId())
                .setMaxIters(defaultInt(agentVO.getMaxIters(), 10))
                .setWorkspacePath(firstText(agentVO.getWorkspacePath(), ".agentscope/workspace"))
                .setPermissionMode(firstText(agentVO.getPermissionMode(), "ASK"))
                .setVisualSchemaJson(agentVO.getVisualSchemaJson())
                .setAgentPermissionPolicyId(agentVO.getAgentPermissionPolicyId())
                .setPublishStatus(defaultInt(agentVO.getPublishStatus(), 0))
                .setContextEnabled(defaultInt(agentVO.getContextEnabled(), 0))
                .setTriggerMessages(defaultInt(agentVO.getTriggerMessages(), 30))
                .setKeepMessages(defaultInt(agentVO.getKeepMessages(), 10))
                .setTriggerTokens(defaultInt(agentVO.getTriggerTokens(), 6000))
                .setKeepTokens(defaultInt(agentVO.getKeepTokens(), 800))
                .setFlushBeforeCompact(defaultInt(agentVO.getFlushBeforeCompact(), 1))
                .setOffloadBeforeCompact(defaultInt(agentVO.getOffloadBeforeCompact(), 1))
                .setCompactionModelConfigId(agentVO.getCompactionModelConfigId())
                .setTruncateArgsEnabled(boolToInt(agentVO.getTruncateArgsEnabled(), 0))
                .setTruncateArgsMaxChars(agentVO.getTruncateArgsMaxChars())
                .setToolResultEvictionEnabled(boolToInt(agentVO.getToolResultEvictionEnabled(), 1))
                .setToolResultMaxChars(agentVO.getToolResultMaxChars())
                .setMemoryEnable(defaultInt(agentVO.getMemoryEnable(), 1))
                .setPlanModeEnabled(defaultInt(agentVO.getPlanModeEnabled(), 1))
                .setPlanFileDirectory(firstText(agentVO.getPlanFileDirectory(), "plans"))
                .setTaskListEnabled(defaultInt(agentVO.getTaskListEnabled(), 1))
                .setAllowShellInPlanMode(defaultInt(agentVO.getAllowShellInPlanMode(), 0))
                .setPlanExitApprovalRequired(defaultInt(agentVO.getPlanExitApprovalRequired(), 1))
                .setPlanMaxSteps(defaultInt(agentVO.getPlanMaxSteps(), 20))
                .setPlanAutoEnterEnabled(defaultInt(agentVO.getPlanAutoEnterEnabled(), 1))
                .setPlanPrompt(agentVO.getPlanPrompt())
                .setSandboxEnabled(defaultInt(agentVO.getSandboxEnabled(), 0))
                .setSandboxConfigId(agentVO.getSandboxConfigId());
        if (Integer.valueOf(1).equals(config.getPublishStatus())) {
            config.setPublishedAt(LocalDateTime.now());
        }
        config.setTenantId(agent.getTenantId());
        agentConfigMapper.insert(EntityDefaults.create(config));

        createKnowledgeBindings(agentVO.getSelectedKnowledgeBaseIds(), agent.getId(), config.getId());
        createSubagentBindings(agentVO.getSelectedSubagentIds(), agent.getId(), config.getId());
        createToolBindings(agentVO.getSelectedToolIds(), agent.getId(), config.getId());
        return true;
    }

    @Override
    public List<AiAgentEntity> subAgentList(Long agentId) {
        return aiAgentMapper.subAgentList(agentId);
    }

    private void createKnowledgeBindings(List<Long> knowledgeBaseIds, Long agentId, Long agentConfigId) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return;
        }
        int priority = 1;
        List<AiKnowledgeBaseEntity> knowledgeBaseEntities = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                .in(AiKnowledgeBaseEntity::getId, knowledgeBaseIds));
        List<AiKnowledgeAgentBindingEntity> knowledgeAgentBinding = new ArrayList<>();
        for (AiKnowledgeBaseEntity knowledgeBase : knowledgeBaseEntities) {
            if (knowledgeBase == null) {
                continue;
            }
            AiKnowledgeAgentBindingEntity binding = new AiKnowledgeAgentBindingEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(agentConfigId)
                    .setKnowledgeBaseId(knowledgeBase.getId())
                    .setRagMode("AGENTIC")
                    .setRetrieveTopK(5)
                    .setScoreThreshold(new BigDecimal("0.50"))
                    .setRerankEnabled((byte) 0)
                    .setStatus((byte) 1);
            knowledgeAgentBinding.add(EntityDefaults.create(binding));
        }
        knowledgeAgentBindingMapper.insert(knowledgeAgentBinding);
    }

    private void createSubagentBindings(List<Long> subagentIds, Long agentId, Long agentConfigId) {
        if (subagentIds == null || subagentIds.isEmpty()) {
            return;
        }
        List<AiSubagentEntity> aiSubagentEntities = subagentMapper
                .selectList(new LambdaQueryWrapper<AiSubagentEntity>()
                .in(AiSubagentEntity::getId, subagentIds));
        List<AiSubagentAgentBindingEntity> subagentAgentBinding = new ArrayList<>();
        for (AiSubagentEntity subagent : aiSubagentEntities) {
            if (subagent == null) {
                continue;
            }
            AiSubagentAgentBindingEntity binding = new AiSubagentAgentBindingEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(agentConfigId)
                    .setSubagentId(subagent.getId())
                    .setEnabled((byte) 1)
                    .setVisibleToParent((byte) 1)
                    .setExposeToUser((byte) 0)
                    .setDefaultTimeoutSeconds(0)
                    .setMaxTimeoutSeconds(300)
                    .setMaxParallelTasks(3)
                    .setInheritParentPermissions((byte) 1)
                    .setInheritParentMemory((byte) 1)
                    .setInheritParentKnowledge((byte) 1);
            subagentAgentBinding.add(EntityDefaults.create(binding));
        }
        subagentAgentBindingMapper.insert(subagentAgentBinding);
    }

    private void createToolBindings(List<Long> toolIds, Long agentId, Long agentConfigId) {
        if (toolIds == null || toolIds.isEmpty()) {
            return;
        }
        List<AiToolInfoConfigEntity> toolList = toolInfoConfigMapper.selectList(new QueryWrapper<AiToolInfoConfigEntity>()
                .in("id", toolIds));
        List<AiAgentToolEntity> AgentToolBinding = new ArrayList<>();
        for (AiToolInfoConfigEntity toolInfo : toolList) {
            if (toolInfo == null) {
                continue;
            }
            AiAgentToolEntity binding = new AiAgentToolEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(agentConfigId)
                    .setToolInfoConfigId(toolInfo.getId())
                    .setToolName(toolInfo.getToolName())
                    .setToolAlias(toolInfo.getToolNameExplain())
                    .setToolDescription(toolInfo.getDescription())
                    .setToolGroup(toolInfo.getGroupId())
                    .setStatus((byte) 1);
            AgentToolBinding.add(EntityDefaults.create(binding));
        }
        agentToolMapper.insert(AgentToolBinding);
    }


}
