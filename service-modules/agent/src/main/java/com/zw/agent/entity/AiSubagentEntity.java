package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import io.agentscope.harness.agent.subagent.WorkspaceMode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_subagent_definition")
public class AiSubagentEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 子Agent定义ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 子Agent唯一编码，例如 reviewer、coder、researcher
     */
    @TableField("subagent_key")
    private String subagentKey;

    /**
     * 子Agent显示名称
     */
    @TableField("subagent_name")
    private String subagentName;

    /**
     * 子Agent能力描述，模型选择是否委派的重要依据
     */
    @TableField("description")
    private String description;

    /**
     * 子Agent系统提示词或spec正文
     */
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * 子Agent模型配置ID，为空则继承父Agent模型
     */
    @TableField("model_config_id")
    private Long modelConfigId;

    /**
     * 子Agent单次最大推理迭代次数
     */
    @TableField("max_steps")
    private Integer maxSteps;

    /**
     * 工作区模式：ISOLATED独立，SHARED共享父工作区
     */
    @TableField("workspace_mode")
    private String workspaceMode;

    /**
     * 子Agent相对工作区路径，可为空
     */
    @TableField("workspace_path")
    private String workspacePath;

    /**
     * 是否允许暴露给用户直接对话：1是，0否，NULL交给调用策略
     */
    @TableField("expose_to_user")
    private Byte exposeToUser;

    /**
     * 是否持久化子Agent会话：1复用，0每次新建
     */
    @TableField("persist_session")
    private Byte persistSession;

    /**
     * 允许使用的工具名数组，例如read_file,grep_files
     */
    @TableField("tool_allow_list")
    private String toolAllowList;

    /**
     * 绑定知识库ID数组
     */
    @TableField("knowledge_base_ids_json")
    private String knowledgeBaseIdsJson;

    /**
     * 子Agent沙箱配置ID，可为空表示继承或禁用
     */
    @TableField("sandbox_config_id")
    private Long sandboxConfigId;

    /**
     * 子Agent权限策略ID，可为空表示继承父级DENY规则
     */
    @TableField("permission_policy_id")
    private Long permissionPolicyId;

    /**
     * 风险等级：LOW/MEDIUM/HIGH/CRITICAL
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * 状态：1启用，0停用
     */
    @TableField("status")
    private Byte status;
}
