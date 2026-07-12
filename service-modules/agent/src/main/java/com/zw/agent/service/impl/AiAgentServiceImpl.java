package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentConfigEntity;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.AiKnowledgeAgentBindingEntity;
import com.zw.agent.entity.AiSubagentAgentBindingEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.mapper.AiAgentConfigMapper;
import com.zw.agent.mapper.AiKnowledgeAgentBindingMapper;
import com.zw.agent.mapper.AiAgentMapper;
import com.zw.agent.mapper.AiSubagentAgentBindingMapper;
import com.zw.agent.service.AiAgentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.support.EntityDefaults;
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
    private final AiSubagentAgentBindingMapper subagentAgentBindingMapper;

    @Override
    public AgentConfigDTO getAgentFullInfo(Long tenantId,Long agentId) {
        return aiAgentMapper.getAgentFullInfo(agentId, tenantId);
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
                .setKeepTokens(defaultInt(agentVO.getKeepTokens(), 0))
                .setFlushBeforeCompact(defaultInt(agentVO.getFlushBeforeCompact(), 1))
                .setOffloadBeforeCompact(defaultInt(agentVO.getOffloadBeforeCompact(), 1))
                .setCompactionModelConfigId(agentVO.getCompactionModelConfigId())
                .setTruncateArgsEnabled(boolToInt(agentVO.getTruncateArgsEnabled(), 0))
                .setTruncateArgsMaxChars(agentVO.getTruncateArgsMaxChars())
                .setToolResultEvictionEnabled(boolToInt(agentVO.getToolResultEvictionEnabled(), 1))
                .setToolResultMaxChars(agentVO.getToolResultMaxChars())
                .setMemoryEnable(defaultInt(agentVO.getMemoryEnable(), 1))
                .setPlanModeEnabled(defaultInt(agentVO.getPlanModeEnabled(), 0))
                .setPlanFileDirectory(firstText(agentVO.getPlanFileDirectory(), "plans"))
                .setTaskListEnabled(defaultInt(agentVO.getTaskListEnabled(), 0))
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
        return true;
    }

    private void createKnowledgeBindings(List<Long> knowledgeBaseIds, Long agentId, Long agentConfigId) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return;
        }
        int priority = 1;
        List<AiKnowledgeAgentBindingEntity> knowledgeAgentBinding = new ArrayList<>();
        for (Long knowledgeBaseId : knowledgeBaseIds) {
            if (knowledgeBaseId == null) {
                continue;
            }
            AiKnowledgeAgentBindingEntity binding = new AiKnowledgeAgentBindingEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(agentConfigId)
                    .setKnowledgeBaseId(knowledgeBaseId)
                    .setRagMode("AGENTIC")
                    .setRetrieveTopK(5)
                    .setScoreThreshold(new BigDecimal("0.50"))
                    .setRerankEnabled((byte) 0)
                    .setPriority(priority++)
                    .setStatus((byte) 1);
            knowledgeAgentBinding.add(EntityDefaults.create(binding));
        }
        knowledgeAgentBindingMapper.insert(knowledgeAgentBinding);
    }

    private void createSubagentBindings(List<Long> subagentIds, Long agentId, Long agentConfigId) {
        if (subagentIds == null || subagentIds.isEmpty()) {
            return;
        }
        List<AiSubagentAgentBindingEntity> subagentAgentBinding = new ArrayList<>();
        for (Long subagentId : subagentIds) {
            if (subagentId == null) {
                continue;
            }
            AiSubagentAgentBindingEntity binding = new AiSubagentAgentBindingEntity()
                    .setAgentId(agentId)
                    .setAgentConfigId(agentConfigId)
                    .setSubagentId(subagentId)
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


}
