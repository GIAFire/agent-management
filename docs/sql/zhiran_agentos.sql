/*
 Navicat Premium Data Transfer

 Source Server         : WSL
 Source Server Type    : MySQL
 Source Server Version : 80410 (8.4.10)
 Source Host           : localhost:3306
 Source Schema         : zhiran_agentos

 Target Server Type    : MySQL
 Target Server Version : 80410 (8.4.10)
 File Encoding         : 65001

 Date: 03/08/2026 21:56:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_agent
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Agent 主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Agent 业务唯一编码，例如 customer-service-agent',
  `agent_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Agent 显示名称',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Agent 描述',
  `agent_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'HARNESS' COMMENT 'Agent 类型：HARNESS 或 REACT；平台默认 HARNESS',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_agent_tenant_code`(`tenant_id` ASC, `agent_code` ASC) USING BTREE,
  INDEX `idx_ai_agent_tenant_updated`(`tenant_id` ASC, `deleted` ASC, `updated_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2083862146467708930 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 定义表：保存一个可视化 Agent 的基础身份信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_binding_agent
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_binding_agent`;
CREATE TABLE `ai_agent_binding_agent`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主子Agent绑定ID',
  `agent_id` bigint NOT NULL COMMENT '主Agent ID',
  `subagent_id` bigint NOT NULL COMMENT '子Agent定义ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_subagent`(`tenant_id` ASC, `agent_id` ASC, `subagent_id` ASC) USING BTREE,
  INDEX `idx_agent_config`(`tenant_id` ASC, `agent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主Agent与子Agent绑定表：定义某个主Agent版本可以委派哪些子Agent及调用策略' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_binding_agent
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_config`;
CREATE TABLE `ai_agent_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Agent 版本主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  `agent_id` bigint NOT NULL COMMENT '关联 ai_agent.id',
  `sys_prompt_id` bigint NULL DEFAULT NULL COMMENT '关联 ai_agent_sys_prompt.id',
  `model_id` bigint NULL DEFAULT NULL COMMENT '关联 ai_agent_model.id',
  `max_iters` int NOT NULL DEFAULT 10 COMMENT 'ReAct 最大循环次数，防止无限工具调用',
  `permission_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ASK' COMMENT '默认权限模式：ALLOW/ASK/DENY/EXPLORE 等平台自定义映射',
  `compaction_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用上下文压缩：1启用，0关闭',
  `trigger_messages` int NULL DEFAULT 30 COMMENT '触发压缩的消息条数，例如30条触发',
  `keep_messages` int NULL DEFAULT 10 COMMENT '压缩后保留最近多少条原文消息，例如10条',
  `trigger_tokens` int NULL DEFAULT 6000 COMMENT '触发压缩的估算token数量',
  `keep_tokens` int NULL DEFAULT 800 COMMENT '压缩后保留最近多少token的原文',
  `tool_result_eviction_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用大工具结果卸载：1启用，0关闭',
  `memory_enable` tinyint NULL DEFAULT 1 COMMENT '是否启用长期记忆:1启用,0禁用',
  `plan_mode_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用Plan Mode：1启用，0关闭',
  `plan_file_directory` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'plans' COMMENT '计划文件目录，相对workspace_path，例如plans',
  `task_list_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用todo_write任务列表：1启用，0关闭',
  `allow_shell_in_plan_mode` tinyint NOT NULL DEFAULT 0 COMMENT 'Plan阶段是否允许shell工具：1允许，0禁止；生产环境建议关闭',
  `state_store_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上下文记忆模式:本地、redis、database',
  `deleted` int NULL DEFAULT 0,
  `version` int NULL DEFAULT 0,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_version`(`agent_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_ai_agent_config_tenant_agent`(`tenant_id` ASC, `agent_id` ASC) USING BTREE,
  INDEX `idx_ai_agent_config_model`(`tenant_id` ASC, `model_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2083862146790670338 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 版本表：保存每次可视化配置发布后的不可变快照' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_config
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_message`;
CREATE TABLE `ai_agent_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户数据隔离',
  `session_id` bigint NOT NULL COMMENT '会话ID，关联ai_agent_session.id',
  `run_id` bigint NULL DEFAULT NULL COMMENT '运行ID，关联ai_agent_run_log.id；用户消息创建后可回填',
  `seq` bigint NOT NULL COMMENT '会话内全局消息序号，用于历史恢复和稳定排序',
  `parent_message_id` bigint NULL DEFAULT NULL COMMENT '父消息ID，例如工具结果关联工具调用消息',
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '协议角色：USER、ASSISTANT、SYSTEM、TOOL',
  `message_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息类型：USER_TEXT、ASSISTANT_THINKING、ASSISTANT_TEXT、TOOL_CALL、TOOL_RESULT、SKILL_CALL、SKILL_RESULT、SUBAGENT_CALL、SUBAGENT_RESULT、PLAN_SNAPSHOT、PLAN_OPERATION、SYSTEM_NOTICE、ERROR',
  `message_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'COMPLETED' COMMENT '消息状态：STREAMING、COMPLETED、FAILED、CANCELLED',
  `sender_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者显示名称，例如用户、Agent、工具、Skill、子Agent名称',
  `content_format` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TEXT' COMMENT '内容格式：TEXT、MARKDOWN、JSON、MULTIMODAL、CARD',
  `text_content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '可搜索和直接展示的文本内容',
  `content_json` json NULL COMMENT '平台标准化内容块或卡片快照，不保存逐Token增量事件',
  `content_schema_version` int NOT NULL DEFAULT 1 COMMENT 'content_json结构版本，用于前端兼容升级',
  `ref_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联业务类型：TOOL_CALL_LOG、SKILL_LOG、SUBAGENT_TASK、AGENT_PLAN、WORKSPACE_FILE',
  `ref_id` bigint NULL DEFAULT NULL COMMENT '关联业务表主键ID',
  `ref_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外部关联键，例如tool_call_id、skill_runtime_id、subagent task_id',
  `external_message_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AgentScope或模型侧原始消息ID',
  `usage_json` json NULL COMMENT 'Token用量，例如inputTokens、outputTokens、reasoningTokens、totalTokens',
  `finish_reason` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型结束原因，例如stop、length、tool_calls',
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '消息生成或执行错误码',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息，返回前端时需要过滤内部敏感信息',
  `started_at` datetime NULL DEFAULT NULL COMMENT '消息块或调用开始时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '消息块或调用结束时间',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '耗时，单位毫秒',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删，1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_session_seq`(`tenant_id` ASC, `session_id` ASC, `seq` ASC) USING BTREE,
  INDEX `idx_run_seq`(`run_id` ASC, `seq` ASC) USING BTREE,
  INDEX `idx_parent_message`(`parent_message_id` ASC) USING BTREE,
  INDEX `idx_ref`(`ref_type` ASC, `ref_id` ASC) USING BTREE,
  INDEX `idx_ref_key`(`ref_type` ASC, `ref_key` ASC) USING BTREE,
  INDEX `idx_tenant_created`(`tenant_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3330 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent消息时间线表：保存用户输入、思考、正文及工具、Skill、子Agent、Plan等可展示消息快照' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_message
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_message_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_message_log`;
CREATE TABLE `ai_agent_message_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户数据隔离',
  `session_id` bigint NOT NULL COMMENT '会话ID，关联ai_agent_session.id',
  `run_id` bigint NULL DEFAULT NULL COMMENT '运行ID，关联ai_agent_run.id；用户消息创建时可为空，后续回填',
  `seq` int NULL DEFAULT NULL COMMENT '会话内消息序号，用于按顺序恢复聊天记录',
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息角色：USER用户，ASSISTANT智能体，SYSTEM系统，TOOL工具',
  `sender_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者名称，例如用户昵称、Agent名称、工具名称',
  `text_content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文本内容，便于列表展示和搜索',
  `content_json` json NULL COMMENT '原始消息内容JSON，用于保存多模态、工具结果、结构化内容',
  `usage_token` int NULL DEFAULT NULL COMMENT 'token用量JSON，例如inputTokens、outputTokens、totalTokens',
  `usage_time` double(10, 2) NULL DEFAULT NULL COMMENT '耗时',
  `finish_reason` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型结束原因，例如stop、length、tool_calls',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_seq`(`session_id` ASC, `seq` ASC) USING BTREE,
  INDEX `idx_run_id`(`run_id` ASC) USING BTREE,
  INDEX `idx_tenant_created`(`tenant_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2078159773531324418 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent消息表：保存用户输入、Assistant回复、工具消息等会话内容' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_message_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_model
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_model`;
CREATE TABLE `ai_agent_model`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型配置主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  `config_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户内唯一的模型配置名称',
  `protocol` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型接口协议：openaiCompatible、dashscope、anthropic、ollama',
  `base_URL` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '兼容 OpenAI 协议时的自定义 baseUrl',
  `api_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'API Key；当前阶段明文存储，仅模型详情接口允许返回',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型简介',
  `model_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '真实模型名称，例如 qwen-plus、gpt-4.1、claude-sonnet-4-5',
  `streaming` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否支持流式',
  `thinking` tinyint(1) NULL DEFAULT NULL COMMENT '是否支持思考',
  `temperature` decimal(4, 3) NULL DEFAULT NULL COMMENT '采样温度，控制随机性',
  `top_p` decimal(4, 3) NULL DEFAULT NULL COMMENT '核采样参数',
  `max_tokens` int NULL DEFAULT NULL COMMENT '最大输出 token 数',
  `timeout_ms` bigint NOT NULL DEFAULT 60000 COMMENT '单次逻辑模型调用超时时间，单位毫秒',
  `max_attempts` int NOT NULL DEFAULT 1 COMMENT '最大尝试次数，包含首次请求',
  `thinking_budget` int NULL DEFAULT NULL COMMENT '思考token预算',
  `fallback_model_config_id` bigint NULL DEFAULT NULL COMMENT '失败时兜底模型配置ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0,
  `deleted` int NULL DEFAULT 0,
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `active_config_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`deleted` = 0) then lower(trim(`config_name`)) else NULL end)) STORED COMMENT '未删除配置名称唯一键' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_agent_model_tenant_status`(`tenant_id` ASC, `status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_ai_agent_model_tenant_provider`(`tenant_id` ASC, `provider_name` ASC) USING BTREE,
  INDEX `idx_ai_agent_model_tenant_protocol`(`tenant_id` ASC, `protocol` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2083862247537852418 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型配置表：把凭证、模型名、生成参数组合成可被 Agent 选择的模型' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_model
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_plan
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_plan`;
CREATE TABLE `ai_agent_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `user_id` bigint NOT NULL COMMENT '用户ID，表示是谁触发了该计划',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置版本ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `run_id` bigint NULL DEFAULT NULL COMMENT '创建或更新该计划的运行ID',
  `plan_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划编号，平台内可读编号，例如PLAN-20260708-0001',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划标题',
  `goal` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户原始目标或Agent总结后的目标',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '计划状态：DRAFT草稿，WAITING_APPROVAL待确认，APPROVED已批准，REJECTED已拒绝，EXECUTING执行中，COMPLETED已完成，FAILED失败，CANCELLED已取消',
  `plan_file_id` bigint NULL DEFAULT NULL COMMENT '关联ai_agent_workspace_file.id，保存PLAN.md文件记录',
  `plan_file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划文件相对路径，例如plans/PLAN.md',
  `plan_content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '计划内容快照，方便后台展示和搜索；完整文件仍以workspace为准',
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'MEDIUM' COMMENT '计划风险等级：LOW/MEDIUM/HIGH/CRITICAL',
  `risk_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '风险摘要，例如会修改哪些资源、调用哪些高危工具',
  `expected_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预期结果或验收标准',
  `approved_by` bigint NULL DEFAULT NULL COMMENT '审批人用户ID',
  `approved_at` datetime NULL DEFAULT NULL COMMENT '审批时间',
  `approval_comment` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批意见',
  `started_at` datetime NULL DEFAULT NULL COMMENT '计划开始执行时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '计划完成时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plan_no`(`tenant_id` ASC, `plan_no` ASC) USING BTREE,
  INDEX `idx_session_status`(`tenant_id` ASC, `session_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_run`(`run_id` ASC) USING BTREE,
  INDEX `idx_agent_config`(`tenant_id` ASC, `agent_id` ASC, `agent_config_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent计划表：保存Plan Mode生成的计划元数据、内容快照、审批和执行状态' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_plan
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_plan_op_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_plan_op_log`;
CREATE TABLE `ai_agent_plan_op_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划操作日志ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '操作用户ID，Agent自动操作时可为空',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置版本ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `run_id` bigint NULL DEFAULT NULL COMMENT '运行ID',
  `plan_id` bigint NULL DEFAULT NULL COMMENT '计划ID',
  `op_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型：PLAN_ENTER，PLAN_WRITE，PLAN_EXIT_REQUEST，PLAN_APPROVE，PLAN_REJECT，TODO_WRITE，PLAN_STATUS_CHANGE',
  `tool_call_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AgentScope工具调用ID',
  `before_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作前计划状态',
  `after_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作后计划状态',
  `payload_json` json NULL COMMENT '操作原始载荷，例如工具入参、任务列表、审批信息',
  `success` tinyint NOT NULL DEFAULT 1 COMMENT '是否成功：1成功，0失败',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败原因',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_plan_time`(`plan_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_run_time`(`run_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_session_time`(`tenant_id` ASC, `session_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_op_type`(`op_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 161 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent计划操作日志表：记录plan_enter、plan_write、plan_exit、todo_write等计划相关事件' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_plan_op_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_plan_task
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_plan_task`;
CREATE TABLE `ai_agent_plan_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划任务ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `plan_id` bigint NOT NULL COMMENT '计划ID，关联ai_agent_plan.id',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `run_id` bigint NULL DEFAULT NULL COMMENT '最近一次更新该任务的运行ID',
  `task_index` int NOT NULL COMMENT '任务序号',
  `subject` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务标题或任务描述',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '任务详细说明',
  `state` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING待执行，IN_PROGRESS执行中，COMPLETED已完成，BLOCKED阻塞，FAILED失败，CANCELLED取消',
  `owner` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务负责人：主Agent、子Agent名称或用户',
  `blocks_json` json NULL COMMENT '该任务阻塞哪些任务ID或序号',
  `blocked_by_json` json NULL COMMENT '该任务被哪些任务阻塞',
  `evidence_json` json NULL COMMENT '完成证据，例如文件ID、工具调用ID、结果摘要',
  `started_at` datetime NULL DEFAULT NULL COMMENT '任务开始时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '任务完成时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plan_task_index`(`plan_id` ASC, `task_index` ASC) USING BTREE,
  INDEX `idx_plan_state`(`plan_id` ASC, `state` ASC) USING BTREE,
  INDEX `idx_session`(`tenant_id` ASC, `session_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 264 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent计划任务表：保存todo_write生成的结构化任务清单和执行状态' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_plan_task
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_run_event_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_run_event_log`;
CREATE TABLE `ai_agent_run_event_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户数据隔离',
  `run_id` bigint NOT NULL COMMENT '运行ID，关联ai_agent_run.id',
  `session_id` bigint NOT NULL COMMENT '会话ID，冗余保存便于查询',
  `event_type` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AgentScope事件类型，例如TEXT_BLOCK_DELTA、TOOL_CALL_START、ERROR',
  `sse_event` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '输出给前端的SSE事件名，例如message、tool_call、error、done',
  `delta_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文本增量，仅文本流事件有值',
  `payload_json` json NULL COMMENT '事件完整JSON，保存AgentScope原始事件或平台标准事件',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `source_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '事件来源路径，NULL表示父Agent，例如main/reviewer表示子Agent事件',
  `subagent_instance_id` bigint NULL DEFAULT NULL COMMENT '子Agent实例ID，父Agent事件为空',
  `subagent_task_id` bigint NULL DEFAULT NULL COMMENT '子Agent任务ID，父Agent事件为空',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2077625970011746798 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent运行事件表：保存流式token、工具调用、权限请求、错误等执行过程事件' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_run_event_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_run_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_run_log`;
CREATE TABLE `ai_agent_run_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '运行主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户数据隔离',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT '本次运行使用的Agent配置版本ID',
  `session_id` bigint NOT NULL COMMENT '会话ID，关联ai_agent_session.id',
  `input_message_id` bigint NULL DEFAULT NULL COMMENT '触发本次运行的用户消息ID',
  `output_message_id` bigint NULL DEFAULT NULL COMMENT '本次运行生成的Assistant消息ID',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '运行状态：RUNNING运行中，SUCCESS成功，FAILED失败，CANCELLED取消',
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误码，便于后端分类处理',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息，保存内部错误，不建议直接返回给前端',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '运行开始时间',
  `ended_at` datetime NULL DEFAULT NULL COMMENT '运行结束时间',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '运行耗时，单位毫秒',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_agent_run_metrics`(`tenant_id` ASC, `started_at` ASC, `status` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_agent_run_agent_page`(`tenant_id` ASC, `agent_id` ASC, `started_at` ASC, `status` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2084076118048841731 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent运行表：一次用户请求对应一次AgentScope streamEvents执行' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_run_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_sandbox_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_sandbox_config`;
CREATE TABLE `ai_agent_sandbox_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '沙箱配置ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置版本ID',
  `enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用沙箱：1启用，0关闭',
  `backend_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DOCKER' COMMENT '沙箱后端：DOCKER/KUBERNETES/E2B/DAYTONA/AGENTRUN',
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '沙箱镜像，例如 ubuntu:24.04、python:3.12-slim、node:20-slim',
  `isolation_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SESSION' COMMENT '隔离范围：SESSION/USER/AGENT/GLOBAL；多租户平台建议默认SESSION',
  `workspace_root` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '/workspace' COMMENT '容器内工作区目录，默认/workspace',
  `memory_size_bytes` bigint NULL DEFAULT NULL COMMENT '容器内存限制，单位字节，例如536870912表示512MB',
  `cpu_count` bigint NULL DEFAULT NULL COMMENT 'CPU限制，例如1、2',
  `disk_size_bytes` bigint NULL DEFAULT NULL COMMENT '磁盘限制，部分后端支持',
  `network_mode` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '网络模式，例如 none/bridge/host；生产环境不建议host',
  `exposed_ports_json` json NULL COMMENT '暴露端口数组，例如[3000,8080]',
  `env_json` json NULL COMMENT '注入容器的环境变量JSON，禁止放明文密钥',
  `snapshot_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NOOP' COMMENT '快照类型：NOOP/LOCAL/REDIS/JDBC/OSS/REMOTE',
  `snapshot_location` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '快照位置，例如本地目录、OSS前缀、Redis前缀',
  `workspace_projection_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用宿主workspace静态资产投影：1启用，0关闭',
  `workspace_projection_roots_json` json NULL COMMENT '投影根路径，例如AGENTS.md、skills、subagents、knowledge',
  `execution_guard_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用并发执行锁：1启用，0关闭',
  `execution_guard_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行锁类型：REDIS/JDBC',
  `execution_guard_ttl_seconds` int NULL DEFAULT 1800 COMMENT '执行锁租约TTL，单位秒',
  `shell_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许Agent执行shell：1允许，0禁止',
  `shell_permission_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ASK' COMMENT 'shell工具权限模式：ALLOW/ASK/DENY',
  `max_command_timeout_seconds` int NOT NULL DEFAULT 120 COMMENT '平台层单条命令最大执行时间，单位秒',
  `max_output_bytes` int NOT NULL DEFAULT 100000 COMMENT '平台层单条命令最大输出字节数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_config_sandbox`(`tenant_id` ASC, `agent_id` ASC, `agent_config_id` ASC) USING BTREE,
  INDEX `idx_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent沙箱配置表：定义Agent代码执行和文件系统隔离策略' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_sandbox_config
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_sandbox_file_sync_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_sandbox_file_sync_log`;
CREATE TABLE `ai_agent_sandbox_file_sync_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '沙箱文件同步日志ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置版本ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `run_id` bigint NOT NULL COMMENT '运行ID',
  `sandbox_instance_id` bigint NULL DEFAULT NULL COMMENT '沙箱实例ID',
  `source_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '沙箱内文件路径',
  `workspace_file_id` bigint NULL DEFAULT NULL COMMENT '同步后的workspace文件ID',
  `sync_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '同步类型：OUTPUT/REPORT/ARTIFACT/LOG/SNAPSHOT',
  `success` tinyint NOT NULL DEFAULT 1 COMMENT '是否成功',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_run`(`run_id` ASC) USING BTREE,
  INDEX `idx_workspace_file`(`workspace_file_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '沙箱文件同步日志表：记录沙箱内产物同步到平台workspace文件表的过程' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_sandbox_file_sync_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_session
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_session`;
CREATE TABLE `ai_agent_session`  (
  `id` bigint NOT NULL COMMENT '会话主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户数据隔离',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID，关联智能体定义表',
  `agent_config_id` bigint NOT NULL COMMENT '本会话使用的Agent配置版本ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，表示该会话归属哪个登录用户',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题，可由第一条用户消息或大模型生成',
  `status` int NOT NULL DEFAULT 1 COMMENT '会话状态：1正常，0关闭',
  `last_message_at` datetime NULL DEFAULT NULL COMMENT '最后一条消息时间，用于会话列表排序',
  `state_store_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AgentState 外部存储 key，后续接 Redis/MySQL 状态存储',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent会话表：保存用户和Agent之间的一段连续对话' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_session
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_state_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_state_log`;
CREATE TABLE `ai_agent_state_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '状态引用主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `run_id` bigint NULL DEFAULT NULL COMMENT '运行时ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置版本ID',
  `session_id` bigint NOT NULL COMMENT '平台会话ID，关联ai_agent_session.id',
  `user_id` bigint NOT NULL COMMENT '平台用户ID',
  `state_backend` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态后端：REDIS/MYSQL/OSS/JSON_FILE/IN_MEMORY',
  `state_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AgentState在后端存储中的key',
  `state_size_bytes` bigint NULL DEFAULT NULL COMMENT '序列化后状态大小，单位字节',
  `context_message_count` int NULL DEFAULT NULL COMMENT 'AgentState上下文消息数量',
  `summary_exists` tinyint NOT NULL DEFAULT 0 COMMENT '是否已经产生上下文摘要：1是，0否',
  `summary_preview` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '摘要预览，用于后台排查，不保存完整敏感内容',
  `last_compacted_at` datetime NULL DEFAULT NULL COMMENT '最后一次上下文压缩时间',
  `last_loaded_at` datetime NULL DEFAULT NULL COMMENT '最后一次加载状态时间',
  `last_saved_at` datetime NULL DEFAULT NULL COMMENT '最后一次保存状态时间',
  `expire_at` datetime NULL DEFAULT NULL COMMENT '状态过期时间，用于清理长时间不用的会话状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NOT NULL DEFAULT 1 COMMENT '状态版本号，每次保存后递增，用于并发与排查',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session`(`session_id` ASC) USING BTREE,
  INDEX `idx_expire_at`(`expire_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 743 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AgentState状态引用表：记录AgentScope运行时状态在外部存储中的位置和元数据' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_state_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_state_op_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_state_op_log`;
CREATE TABLE `ai_agent_state_op_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '状态操作日志主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `session_id` bigint NOT NULL COMMENT '平台会话ID',
  `run_id` bigint NULL DEFAULT NULL COMMENT 'Agent运行ID',
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'userId',
  `op_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型：LOAD/SAVE/COMPACT/EVICT/DELETE/EXPIRE',
  `state_backend` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态后端：REDIS/MYSQL/OSS/JSON_FILE/IN_MEMORY',
  `state_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态存储key',
  `before_size_bytes` bigint NULL DEFAULT NULL COMMENT '操作前状态大小，单位字节',
  `after_size_bytes` bigint NULL DEFAULT NULL COMMENT '操作后状态大小，单位字节',
  `before_message_count` int NULL DEFAULT NULL COMMENT '操作前上下文消息数',
  `after_message_count` int NULL DEFAULT NULL COMMENT '操作后上下文消息数',
  `success` tinyint NOT NULL DEFAULT 1 COMMENT '是否成功：1成功，0失败',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_time`(`session_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_run_time`(`run_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_op_type`(`op_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1702 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AgentState操作日志表：记录状态加载、保存、压缩、清理等操作' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_state_op_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_sys_prompt
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_sys_prompt`;
CREATE TABLE `ai_agent_sys_prompt`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提示词模板主键ID',
  `prompt_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提示词模板名称',
  `description` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '描述',
  `sys_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '提示词内容',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用，2草稿',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2071009554372964354 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 定义表：保存一个可视化 Agent 的基础身份信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_sys_prompt
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_tool
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_tool`;
CREATE TABLE `ai_agent_tool`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Agent与Tool绑定主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `agent_id` bigint NOT NULL COMMENT 'AgentID，关联 ai_agent.id',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置ID，关联 ai_agent_config.id',
  `tool_info_config_id` bigint NOT NULL COMMENT '工具ID，关联 tool_info_config_.id',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具函数名，必须与AgentScope注册到Toolkit中的工具名一致',
  `tool_alias` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具中文名，可用于前端展示或给Agent暴露不同名称',
  `tool_description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具描述',
  `tool_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具分组，例如database、workspace、rag、sandbox、business',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常，0删除或停用',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2084074761686425602 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent与Tool绑定表：定义某个Agent启用了哪些工具' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_tool
-- ----------------------------

-- ----------------------------
-- Table structure for ai_agent_workspace_file
-- ----------------------------
DROP TABLE IF EXISTS `ai_agent_workspace_file`;
CREATE TABLE `ai_agent_workspace_file`  (
  `id` bigint NOT NULL COMMENT '工作区文件ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置版本ID',
  `session_id` bigint NULL DEFAULT NULL COMMENT '运行时会话ID',
  `run_id` bigint NULL DEFAULT NULL COMMENT '运行ID',
  `tool_call_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具调用ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '展示文件名',
  `relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '相对workspace_path的路径',
  `storage_backend` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/OSS/MINIO/S3',
  `storage_key` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实存储key，本地可存绝对路径或相对路径',
  `mime_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MIME类型',
  `file_ext` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件扩展名',
  `size_bytes` bigint NULL DEFAULT NULL COMMENT '文件大小',
  `checksum` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件SHA256',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件标题',
  `summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件摘要',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源：AGENT/TOOL/USER_UPLOAD/SYSTEM/SKILL',
  `visibility` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SESSION' COMMENT 'PRIVATE/SESSION/AGENT/TENANT',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `skill_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'skillID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_file`(`tenant_id` ASC, `session_id` ASC) USING BTREE,
  INDEX `idx_run_file`(`tenant_id` ASC, `run_id` ASC) USING BTREE,
  INDEX `idx_agent_file`(`tenant_id` ASC, `agent_id` ASC, `agent_config_id` ASC) USING BTREE,
  INDEX `idx_type`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent工作区文件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_agent_workspace_file
-- ----------------------------

-- ----------------------------
-- Table structure for ai_http_header
-- ----------------------------
DROP TABLE IF EXISTS `ai_http_header`;
CREATE TABLE `ai_http_header`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `source_id` bigint NOT NULL COMMENT '关联外部资源表id',
  `source` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '值来源：model、subAgent',
  `header_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请求头名称，例如Authorization、X-API-Key',
  `header_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求头值；当前阶段明文存储，仅所属资源详情接口允许返回',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_http_header_source`(`tenant_id` ASC, `source` ASC, `source_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'HTTP请求头配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_http_header
-- ----------------------------

-- ----------------------------
-- Table structure for ai_knowledge_agent_binding
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge_agent_binding`;
CREATE TABLE `ai_knowledge_agent_binding`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Agent知识库绑定主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置版本ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库ID，关联ai_knowledge_base.id',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用，2删除',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_kb`(`tenant_id` ASC, `agent_id` ASC, `agent_config_id` ASC, `knowledge_base_id` ASC) USING BTREE COMMENT '同一Agent配置不能重复绑定同一知识库',
  INDEX `idx_agent_config`(`tenant_id` ASC, `agent_id` ASC, `agent_config_id` ASC, `status` ASC) USING BTREE COMMENT '按Agent配置查询绑定知识库索引',
  INDEX `idx_kb`(`knowledge_base_id` ASC, `status` ASC) USING BTREE COMMENT '按知识库查询绑定关系索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2083958466641629186 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent知识库绑定表：控制Agent配置可访问的知识库及检索参数' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_knowledge_agent_binding
-- ----------------------------

-- ----------------------------
-- Table structure for ai_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge_base`;
CREATE TABLE `ai_knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，用于用户隔离',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `knowledge_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库名称，例如产品文档库、售后政策库',
  `knowledge_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库英文名称',
  `collection_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '集合名称',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库说明，描述用途和内容范围',
  `chunk_strategy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'GENERAL' COMMENT '切片策略：GENERAL/PARAGRAPH/TOKEN/QA/TABLE/PDF_LAYOUT/CUSTOM',
  `chunk_size` int NULL DEFAULT 800 COMMENT '切片大小，单位可按字符或token，由实现决定',
  `chunk_overlap` int NULL DEFAULT 120 COMMENT '切片重叠大小，用于保留上下文连续性',
  `rerank_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用rerank：1启用，0关闭',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用，2删除',
  `provider_meta_json` json NULL COMMENT '后端特定元信息，例如RAGFlow dataset配置、索引版本等',
  `backend_store_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '后端类型:如本地、百炼、RAGFlow、Dify',
  `api_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本地后端API类型:如openai、ollama',
  `endpoint` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'RAG服务访问地址，例如RAGFlow地址、api_url',
  `endpoint_port` int NULL DEFAULT NULL COMMENT '向量库或RAG服务访问端口号',
  `model_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量模型url',
  `api_key` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Embedding API Key，当前明文保存，查询接口禁止返回',
  `embedding_model_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Embedding模型名称，例如 text-embedding-v3',
  `embedding_dimension` int NULL DEFAULT NULL COMMENT '向量维度，例如768、1024、1536',
  `metric_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '距离度量：COSINE/IP/L2/BM25/HYBRID',
  `top_k` int NOT NULL DEFAULT 5 COMMENT '检索返回结果数量',
  `score_threshold` decimal(6, 4) NULL DEFAULT 0.3000 COMMENT '相似度阈值，低于该分数不返回',
  `config_json` json NULL COMMENT '后端扩展配置JSON，例如rerank配置、hybrid检索权重、RAGFlow参数',
  `active_knowledge_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (coalesce(`deleted`,0) = 0) then lower(trim(`knowledge_name`)) else NULL end)) STORED COMMENT '用于约束同一租户内有效知识库名称唯一' NULL,
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_knowledge_base_active_name`(`tenant_id` ASC, `active_knowledge_name` ASC) USING BTREE,
  INDEX `idx_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE COMMENT '按租户和状态查询知识库索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2084276685990432770 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库表：平台知识库抽象层，兼容RAGFlow及不同向量库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_knowledge_base
-- ----------------------------

-- ----------------------------
-- Table structure for ai_knowledge_chunk
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge_chunk`;
CREATE TABLE `ai_knowledge_chunk`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识切片主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库ID，关联ai_knowledge_base.id',
  `document_id` bigint NOT NULL COMMENT '文档ID，关联ai_knowledge_document.id',
  `chunk_index` int NOT NULL COMMENT '切片序号，从0或1开始',
  `chunk_uid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '切片唯一标识，可用于幂等同步',
  `external_chunk_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外部切片ID，例如RAGFlow chunk_id、向量库point_id',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '知识切片完整文本',
  `content_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '切片内容哈希，用于去重和变更检测',
  `content_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'TEXT' COMMENT '切片内容类型：TEXT/TABLE/IMAGE/OCR/QA/CODE',
  `page_no` int NULL DEFAULT NULL COMMENT '页码，适用于PDF/DOCX等文档',
  `section_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '章节标题或段落标题',
  `start_offset` int NULL DEFAULT NULL COMMENT '切片在原文中的开始位置',
  `end_offset` int NULL DEFAULT NULL COMMENT '切片在原文中的结束位置',
  `token_count` int NULL DEFAULT NULL COMMENT '切片估算token数量',
  `vector_store_id` bigint NULL DEFAULT NULL COMMENT '向量存储配置ID，关联ai_knowledge_vector_config.id',
  `vector_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量库中的向量ID或point ID',
  `embedding_model_config_id` bigint NULL DEFAULT NULL COMMENT 'Embedding模型配置ID',
  `embedding_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `metadata_json` json NULL COMMENT '切片元信息JSON，例如表格结构、图片坐标、页码、标题路径',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用，2删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_doc_chunk_index`(`document_id` ASC, `chunk_index` ASC) USING BTREE COMMENT '同一文档下切片序号唯一',
  INDEX `idx_kb_doc`(`knowledge_base_id` ASC, `document_id` ASC) USING BTREE COMMENT '按知识库和文档查询切片索引',
  INDEX `idx_vector_id`(`vector_store_id` ASC, `vector_id` ASC) USING BTREE COMMENT '按向量库ID查询切片索引',
  INDEX `idx_external_chunk`(`external_chunk_id` ASC) USING BTREE COMMENT '按外部切片ID查询索引',
  INDEX `idx_content_hash`(`tenant_id` ASC, `content_hash` ASC) USING BTREE COMMENT '按租户和内容哈希查询索引',
  INDEX `idx_ai_knowledge_chunk_document`(`tenant_id` ASC, `document_id` ASC, `deleted` ASC, `chunk_index` ASC) USING BTREE,
  INDEX `idx_ai_knowledge_chunk_task`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2084276780236443651 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识切片表：记录文档切片内容、向量ID及引用元信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_knowledge_chunk
-- ----------------------------

-- ----------------------------
-- Table structure for ai_knowledge_document
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge_document`;
CREATE TABLE `ai_knowledge_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库文档主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `knowledge_base_id` bigint NOT NULL COMMENT '知识库ID，关联ai_knowledge_base.id',
  `workspace_file_id` bigint NULL DEFAULT NULL COMMENT '来源工作区文件ID，关联ai_agent_workspace_file.id',
  `external_document_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外部文档ID，例如RAGFlow document_id、向量库文档ID',
  `document_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档名称，用于展示和检索',
  `document_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档类型：PDF/DOCX/XLSX/TXT/MD/HTML/CSV/JSON/URL',
  `mime_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MIME类型，例如application/pdf、text/markdown',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WORKSPACE_FILE' COMMENT '来源类型：WORKSPACE_FILE/UPLOAD/URL/API/MANUAL',
  `source_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源地址，例如文件路径、URL、对象存储key',
  `size_bytes` bigint NULL DEFAULT NULL COMMENT '文档大小，单位字节',
  `checksum` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档内容校验值，例如SHA256',
  `language` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档语言，例如zh-CN、en-US',
  `version_no` int NOT NULL DEFAULT 1 COMMENT '文档版本号，重新上传或重建时递增',
  `parse_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '解析状态：PENDING待处理、UPLOADED已上传、PARSING解析中、CHUNKING切片中、EMBEDDING向量化中、READY可检索、FAILED失败',
  `chunk_count` int NOT NULL DEFAULT 0 COMMENT '切片数量',
  `token_count` int NULL DEFAULT NULL COMMENT '文档估算token数量',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处理失败原因',
  `lease_owner` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '后台处理租约所有者',
  `lease_until` datetime NULL DEFAULT NULL COMMENT '后台处理租约到期时间',
  `provider_meta_json` json NULL COMMENT '后端特定元信息，例如RAGFlow解析进度、缩略图、OCR配置',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用，2删除',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `chunk_strategy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生效切片策略：CHARACTER/PARAGRAPH/DELIMITER',
  `chunk_size` int NULL DEFAULT NULL COMMENT 'CHARACTER/PARAGRAPH 最大字符数',
  `chunk_overlap` int NULL DEFAULT NULL COMMENT 'CHARACTER/PARAGRAPH 重叠字符数',
  `chunk_delimiter` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'DELIMITER 策略使用的实际分隔字符串',
  `active_document_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (coalesce(`deleted`,0) = 0) then lower(trim(`document_name`)) else NULL end)) STORED COMMENT '用于约束同一知识库内有效文档名称唯一' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_knowledge_document_active_name`(`tenant_id` ASC, `knowledge_base_id` ASC, `active_document_name` ASC) USING BTREE,
  INDEX `idx_kb_status`(`knowledge_base_id` ASC, `parse_status` ASC) USING BTREE COMMENT '按知识库和解析状态查询文档索引',
  INDEX `idx_workspace_file`(`workspace_file_id` ASC) USING BTREE COMMENT '按工作区文件查询文档索引',
  INDEX `idx_external_doc`(`external_document_id` ASC) USING BTREE COMMENT '按外部文档ID查询索引',
  INDEX `idx_checksum`(`tenant_id` ASC, `checksum` ASC) USING BTREE COMMENT '按租户和文件校验值去重索引',
  INDEX `idx_ai_knowledge_document_base_status`(`tenant_id` ASC, `knowledge_base_id` ASC, `parse_status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ai_knowledge_document_work_queue`(`deleted` ASC, `status` ASC, `parse_status` ASC, `lease_until` ASC, `updated_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2084276735516774402 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库文档表：记录平台文档与外部RAG/向量库文档的映射关系' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_knowledge_document
-- ----------------------------

-- ----------------------------
-- Table structure for ai_knowledge_retrieval_hit
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge_retrieval_hit`;
CREATE TABLE `ai_knowledge_retrieval_hit`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '检索命中明细主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `retrieval_log_id` bigint NOT NULL COMMENT '检索日志ID，关联ai_knowledge_retrieval_log.id',
  `knowledge_base_id` bigint NULL DEFAULT NULL COMMENT '知识库ID',
  `document_id` bigint NULL DEFAULT NULL COMMENT '文档ID',
  `chunk_id` bigint NULL DEFAULT NULL COMMENT '切片ID',
  `rank_no` int NOT NULL COMMENT '召回排名，从1开始',
  `score` decimal(10, 6) NULL DEFAULT NULL COMMENT '最终相似度分数',
  `vector_score` decimal(10, 6) NULL DEFAULT NULL COMMENT '向量相似度分数',
  `keyword_score` decimal(10, 6) NULL DEFAULT NULL COMMENT '关键词或BM25分数',
  `rerank_score` decimal(10, 6) NULL DEFAULT NULL COMMENT 'rerank分数',
  `document_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '命中文档名称',
  `source_ref` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源引用，例如页码、章节、URL、文件路径',
  `used_in_prompt` tinyint NOT NULL DEFAULT 1 COMMENT '是否最终注入模型上下文：1是，0否',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_log_rank`(`retrieval_log_id` ASC, `rank_no` ASC) USING BTREE COMMENT '按检索日志和排名查询命中结果索引',
  INDEX `idx_chunk`(`chunk_id` ASC) USING BTREE COMMENT '按平台切片ID查询命中记录索引',
  INDEX `idx_doc`(`document_id` ASC) USING BTREE COMMENT '按文档ID查询命中记录索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库检索命中明细表：记录每次检索命中的文档切片、分数和引用信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_knowledge_retrieval_hit
-- ----------------------------

-- ----------------------------
-- Table structure for ai_model_call_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_model_call_log`;
CREATE TABLE `ai_model_call_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型调用日志主键ID',
  `tenant_id` bigint NOT NULL COMMENT '所属租户ID',
  `model_config_id` bigint NULL DEFAULT NULL COMMENT '关联 ai_agent_model.id；未保存配置的手工测试为空',
  `run_id` bigint NULL DEFAULT NULL COMMENT '关联 ai_agent_run_log.id；手工测试为空',
  `session_id` bigint NULL DEFAULT NULL COMMENT '关联 ai_agent_session.id；手工测试为空',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '实际发起调用的智能体ID',
  `agent_config_id` bigint NULL DEFAULT NULL COMMENT '实际发起调用的智能体配置ID',
  `call_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用来源：AGENT_RUN、MANUAL_TEST',
  `source_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '运行时来源路径，用于区分主智能体与本地子智能体',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态：RUNNING、SUCCESS、FAILED、CANCELLED',
  `config_name_snapshot` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用时的模型配置名称快照',
  `protocol_snapshot` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用时的接口协议快照',
  `model_name_snapshot` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用时的真实模型标识快照',
  `input_tokens` int NULL DEFAULT NULL COMMENT '输入 Token 数',
  `output_tokens` int NULL DEFAULT NULL COMMENT '输出 Token 数',
  `cached_tokens` int NULL DEFAULT NULL COMMENT '缓存 Token 数',
  `total_tokens` int NULL DEFAULT NULL COMMENT '总 Token 数',
  `started_at` datetime(3) NOT NULL COMMENT '调用开始时间',
  `ended_at` datetime(3) NULL DEFAULT NULL COMMENT '调用结束时间',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '调用耗时，包含内部自动重试',
  `error_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '脱敏后的错误码',
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '脱敏后的错误摘要',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '0未删，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_model_call_metrics`(`tenant_id` ASC, `call_source` ASC, `started_at` ASC, `status` ASC) USING BTREE,
  INDEX `idx_model_call_model_page`(`tenant_id` ASC, `model_config_id` ASC, `started_at` ASC) USING BTREE,
  INDEX `idx_model_call_run`(`tenant_id` ASC, `run_id` ASC) USING BTREE,
  INDEX `idx_model_call_session`(`tenant_id` ASC, `session_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 64 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型调用审计表：一条记录对应一次逻辑模型生成请求' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_model_call_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_skill_agent_binding
-- ----------------------------
DROP TABLE IF EXISTS `ai_skill_agent_binding`;
CREATE TABLE `ai_skill_agent_binding`  (
  `id` bigint NOT NULL COMMENT 'Agent与Skill绑定ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `agent_id` bigint NOT NULL COMMENT 'AgentID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置ID',
  `skill_id` bigint NOT NULL COMMENT 'SkillID',
  `install_user_id` bigint NULL DEFAULT NULL COMMENT '用户级Skill所属用户ID；非USER安装为空',
  `load_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DYNAMIC' COMMENT '加载模式：DYNAMIC每轮动态合并，BUILD_ONCE构建时合并一次',
  `override_policy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ALLOW_OVERRIDE' COMMENT '同名覆盖策略：ALLOW_OVERRIDE允许高优先级覆盖，DENY_OVERRIDE禁止覆盖',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_config_skill`(`tenant_id` ASC, `agent_config_id` ASC, `skill_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent与Skill绑定表：定义某个Agent配置版本安装哪些Skill以及安装作用域' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_skill_agent_binding
-- ----------------------------

-- ----------------------------
-- Table structure for ai_skill_info
-- ----------------------------
DROP TABLE IF EXISTS `ai_skill_info`;
CREATE TABLE `ai_skill_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Skill定义ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Skill显示名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Skill描述，用于Agent判断何时使用该Skill',
  `skill_content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SKILL.md正文',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源文件目录',
  `metadata_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '完整的 YAML 元数据，以 JSON 形式保存',
  `role_code` int NULL DEFAULT 0 COMMENT '角色code',
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOW' COMMENT '风险等级：LOW/MEDIUM/HIGH/CRITICAL',
  `tags_json` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签JSON数组，例如[\"代码审查\",\"Java\",\"Spring\"]',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_info_code`(`tenant_id` ASC, `source` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2083747229076996098 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Skill定义表：保存可复用能力包的基础信息和当前发布版本' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_skill_info
-- ----------------------------

-- ----------------------------
-- Table structure for ai_skill_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_skill_log`;
CREATE TABLE `ai_skill_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Skill使用日志ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `run_id` bigint NOT NULL COMMENT '运行ID',
  `skill_id` bigint NULL DEFAULT NULL COMMENT 'SkillID，可能从skillId解析失败时为空',
  `skill_runtime_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AgentScope运行时skill-id，例如code-reviewer_workspace-namespaced',
  `tool_call_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AgentScope 工具调用ID',
  `operation` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型：LIST_AVAILABLE_SKILLS，LOAD_SKILL，READ_REFERENCE，RUN_SCRIPT',
  `resource_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '被读取的技能内相对路径',
  `success` tinyint NOT NULL DEFAULT 1 COMMENT '是否成功：1成功，0失败',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败信息',
  `started_at` datetime NOT NULL COMMENT '技能使用开始时间',
  `duration_ms` bigint NOT NULL DEFAULT 0 COMMENT '技能使用耗时（毫秒）',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_log_tool_call`(`tenant_id` ASC, `tool_call_id` ASC) USING BTREE,
  INDEX `idx_run`(`run_id` ASC) USING BTREE,
  INDEX `idx_skill_time`(`tenant_id` ASC, `skill_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_agent_time`(`tenant_id` ASC, `agent_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_skill_log_started`(`tenant_id` ASC, `started_at` ASC) USING BTREE,
  INDEX `idx_skill_log_skill_started`(`tenant_id` ASC, `skill_id` ASC, `started_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Skill使用日志表：记录Agent读取、加载、执行Skill的行为' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_skill_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_skill_resource
-- ----------------------------
DROP TABLE IF EXISTS `ai_skill_resource`;
CREATE TABLE `ai_skill_resource`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Skill文件ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `skill_id` bigint NOT NULL COMMENT 'SkillID',
  `file_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件角色：REFERENCE参考资料，SCRIPT脚本，EXAMPLE样例，ASSET资源',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `resource_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文件内容',
  `resource_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '相对Skill目录路径，例如references/style-guide.md、scripts/run-checks.sh',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  `resource_path_hash` binary(32) GENERATED ALWAYS AS (unhex(sha2(`resource_path`,256))) STORED COMMENT '资源相对路径哈希' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_resource_path`(`tenant_id` ASC, `skill_id` ASC, `resource_path_hash` ASC) USING BTREE,
  INDEX `idx_file_role`(`tenant_id` ASC, `file_role` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2082988320587878402 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_skill_resource
-- ----------------------------

-- ----------------------------
-- Table structure for ai_skill_role
-- ----------------------------
DROP TABLE IF EXISTS `ai_skill_role`;
CREATE TABLE `ai_skill_role`  (
  `id` bigint NOT NULL COMMENT 'Skill定义ID',
  `skill_info_id` bigint NULL DEFAULT NULL COMMENT 'skill_info_id',
  `role_code` int NULL DEFAULT 0 COMMENT '角色code',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_role_scope`(`tenant_id` ASC, `skill_info_id` ASC, `role_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Skill角色权限表：可配置skill角色权限' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_skill_role
-- ----------------------------

-- ----------------------------
-- Table structure for ai_state_store
-- ----------------------------
DROP TABLE IF EXISTS `ai_state_store`;
CREATE TABLE `ai_state_store`  (
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_index` int NOT NULL DEFAULT 0,
  `state_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_id`, `state_key`, `item_index`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_state_store
-- ----------------------------

-- ----------------------------
-- Table structure for ai_subagent
-- ----------------------------
DROP TABLE IF EXISTS `ai_subagent`;
CREATE TABLE `ai_subagent`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '子Agent定义ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `subagent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子Agent唯一标识，例如 remote-researcher',
  `subagent_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子Agent显示名称',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '能力描述，供主Agent判断何时委派',
  `source_type` tinyint NOT NULL COMMENT '来源类型：1.平台Agent 2.远程Agent Protocol',
  `local_agent_id` bigint NULL DEFAULT NULL COMMENT '平台内部子Agent ID，source_type=1时使用，关联ai_agent.id',
  `remote_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '远程Agent Protocol服务基础URL，source_type=2时使用',
  `protocol_type` tinyint NULL DEFAULT NULL COMMENT '远程协议类型：1.Agent Protocol，预留2.A2A',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0.否 1.是',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '子Agent定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_subagent
-- ----------------------------

-- ----------------------------
-- Table structure for ai_subagent_agent_binding
-- ----------------------------
DROP TABLE IF EXISTS `ai_subagent_agent_binding`;
CREATE TABLE `ai_subagent_agent_binding`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主子Agent绑定ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `agent_id` bigint NOT NULL COMMENT '主Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT '主Agent配置版本ID',
  `subagent_id` bigint NOT NULL COMMENT '子Agent定义ID',
  `alias` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定别名；为空则使用subagent_key',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `visible_to_parent` tinyint NOT NULL DEFAULT 1 COMMENT '是否暴露给主Agent选择：1是，0否',
  `expose_to_user` tinyint NULL DEFAULT NULL COMMENT '是否允许暴露给用户直接对话：1是，0否，NULL沿用子Agent定义',
  `default_timeout_seconds` int NOT NULL DEFAULT 30 COMMENT '默认同步等待时间；0表示默认后台任务',
  `max_timeout_seconds` int NOT NULL DEFAULT 600 COMMENT '允许的最大等待时间',
  `max_parallel_tasks` int NOT NULL DEFAULT 3 COMMENT '该子Agent最大并行后台任务数',
  `inherit_parent_permissions` tinyint NOT NULL DEFAULT 1 COMMENT '是否继承父Agent权限限制：1继承，0不继承；生产建议继承',
  `inherit_parent_memory` tinyint NOT NULL DEFAULT 0 COMMENT '是否继承父Agent长期记忆：1继承，0不继承',
  `inherit_parent_knowledge` tinyint NOT NULL DEFAULT 0 COMMENT '是否继承父Agent知识库：1继承，0不继承',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_subagent`(`tenant_id` ASC, `agent_id` ASC, `agent_config_id` ASC, `subagent_id` ASC) USING BTREE,
  INDEX `idx_agent_config`(`tenant_id` ASC, `agent_id` ASC, `agent_config_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主Agent与子Agent绑定表：定义某个主Agent版本可以委派哪些子Agent及调用策略' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_subagent_agent_binding
-- ----------------------------

-- ----------------------------
-- Table structure for ai_subagent_instance
-- ----------------------------
DROP TABLE IF EXISTS `ai_subagent_instance`;
CREATE TABLE `ai_subagent_instance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '子Agent运行实例ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `parent_agent_id` bigint NOT NULL COMMENT '父Agent ID',
  `parent_agent_config_id` bigint NOT NULL COMMENT '父Agent配置版本ID',
  `parent_session_id` bigint NOT NULL COMMENT '父会话ID',
  `parent_run_id` bigint NULL DEFAULT NULL COMMENT '触发创建的父运行ID',
  `subagent_id` bigint NOT NULL COMMENT '子Agent定义ID',
  `subagent_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子Agent编码',
  `label` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'LLM或平台指定的人类可读标签',
  `agent_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AgentScope返回的运行时实例句柄agent_key',
  `subagent_external_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '暴露给前端或Channel的subagentId',
  `runtime_user_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'RuntimeContext.userId',
  `runtime_session_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子Agent运行时sessionKey',
  `workspace_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ISOLATED/SHARED',
  `workspace_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子Agent实际工作区路径',
  `expose_to_user` tinyint NOT NULL DEFAULT 0 COMMENT '是否已暴露给用户直接交互',
  `persist_session` tinyint NOT NULL DEFAULT 0 COMMENT '是否持久会话',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/IDLE/COMPLETED/FAILED/CANCELLED/EXPIRED',
  `started_at` datetime NULL DEFAULT NULL COMMENT '启动时间',
  `last_active_at` datetime NULL DEFAULT NULL COMMENT '最后活跃时间',
  `ended_at` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_session`(`tenant_id` ASC, `parent_session_id` ASC) USING BTREE,
  INDEX `idx_parent_run`(`parent_run_id` ASC) USING BTREE,
  INDEX `idx_agent_key`(`agent_key` ASC) USING BTREE,
  INDEX `idx_subagent_external_id`(`subagent_external_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '子Agent运行实例表：记录父Agent创建的子Agent实例、会话、工作区和暴露状态' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_subagent_instance
-- ----------------------------

-- ----------------------------
-- Table structure for ai_subagent_task
-- ----------------------------
DROP TABLE IF EXISTS `ai_subagent_task`;
CREATE TABLE `ai_subagent_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '子Agent任务ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `parent_agent_id` bigint NOT NULL COMMENT '父Agent ID',
  `parent_agent_config_id` bigint NOT NULL COMMENT '父Agent配置版本ID',
  `parent_session_id` bigint NOT NULL COMMENT '父会话ID',
  `parent_run_id` bigint NOT NULL COMMENT '父Agent运行ID',
  `subagent_instance_id` bigint NULL DEFAULT NULL COMMENT '子Agent实例ID',
  `subagent_id` bigint NOT NULL COMMENT '子Agent定义ID',
  `subagent_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子Agent编码',
  `task_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AgentScope后台任务ID，timeout_seconds=0时返回',
  `task_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SYNC同步，BACKGROUND后台',
  `task_input` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '委派给子Agent的任务内容',
  `task_result` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '子Agent最终结果摘要',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/COMPLETED/FAILED/CANCELLED/TIMEOUT',
  `timeout_seconds` int NULL DEFAULT NULL COMMENT '任务超时时间',
  `token_usage_json` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子Agent token用量',
  `cost_amount` decimal(12, 6) NULL DEFAULT NULL COMMENT '估算成本',
  `started_at` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '耗时毫秒',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_run`(`parent_run_id` ASC) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_status`(`tenant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_subagent_time`(`tenant_id` ASC, `subagent_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '子Agent任务表：记录agent_spawn/agent_send产生的同步或后台委派任务' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_subagent_task
-- ----------------------------

-- ----------------------------
-- Table structure for ai_tool_call_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_call_log`;
CREATE TABLE `ai_tool_call_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具调用审计主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `run_id` bigint NOT NULL COMMENT 'Agent运行ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `agent_config_id` bigint NOT NULL COMMENT 'Agent配置版本ID',
  `user_id` bigint NOT NULL COMMENT '触发用户ID',
  `tool_id` bigint NULL DEFAULT NULL COMMENT '工具ID',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `tool_call_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AgentScope工具调用ID',
  `permission_behavior` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最终权限结果：ALLOW/DENY/ASK/PASSTHROUGH',
  `tool_input_json` json NULL COMMENT '工具入参JSON，敏感字段需要脱敏',
  `tool_output_json` json NULL COMMENT '工具输出JSON，敏感字段需要脱敏或截断',
  `success_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行状态',
  `started_at` datetime NULL DEFAULT NULL COMMENT '工具开始执行时间',
  `ended_at` datetime NULL DEFAULT NULL COMMENT '工具结束执行时间',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '工具执行耗时，单位毫秒',
  `reply_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '回复消息ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2084076338358853634 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工具调用审计表：记录Agent每一次工具调用的权限结果、参数、结果和耗时' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_tool_call_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_tool_group_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_group_config`;
CREATE TABLE `ai_tool_group_config`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具组名称',
  `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具组描述',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `active_by_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认激活',
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_enabled_deleted`(`enabled` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工具组配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_tool_group_config
-- ----------------------------

-- ----------------------------
-- Table structure for ai_tool_info_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_info_config`;
CREATE TABLE `ai_tool_info_config`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NULL DEFAULT 0 COMMENT '关联 ai_agent_definition.id',
  `group_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户侧工具组id，可覆盖 tool_config.default_group_code',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具显示名称',
  `tool_name_explain` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具名称解释',
  `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具描述，给模型看的能力说明',
  `tool_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类全限定名 + 方法名 + 参数类型列表',
  `tool_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'JAVA_BEAN' COMMENT '工具类型：JAVA_BEAN、MCP、HTTP、RPC、SQL 等',
  `bean_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Spring Bean 名称，例如 orderTools',
  `class_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具所在 Java 类全限定名',
  `method_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具方法名',
  `input_schema` json NULL COMMENT '工具入参 JSON Schema，可选，通常可由 @Tool/@ToolParam 生成',
  `output_schema` json NULL COMMENT '工具出参 Schema，可选',
  `permission_code` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务权限编码，例如 order:query',
  `signature_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'sha256签名,用于判断tool是否修改',
  `read_only` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否只读工具：1是，0否',
  `concurrency` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否并发调用：1是，0否',
  `state_injected` tinyint(1) NULL DEFAULT 0 COMMENT '是否在调用时注入 AgentState 作为额外参数（默认 false）',
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'LOW' COMMENT '风险等级：LOW、MEDIUM、HIGH',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `timeout_ms` int UNSIGNED NULL DEFAULT 30000 COMMENT '工具调用超时时间，单位毫秒',
  `max_retries` int UNSIGNED NULL DEFAULT 0 COMMENT '最大重试次数',
  `match_rule` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Rule命中规则',
  `default_group_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认工具组编码',
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：1删除，0正常',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tool_key`(`tool_key` ASC) USING BTREE,
  INDEX `idx_tool_type`(`tool_type` ASC) USING BTREE,
  INDEX `idx_default_group_code`(`default_group_code` ASC) USING BTREE,
  INDEX `idx_enabled_deleted`(`enabled` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2084276468633210889 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '全局工具配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_tool_info_config
-- ----------------------------
INSERT INTO `ai_tool_info_config` VALUES (2084276468633210881, 0, NULL, 'generate_file', 'generate_file', '写文件、生成文件、生成报告', 'com.zw.agent.tools.commonTool.GenerateFileTool#callAsync(io.agentscope.core.tool.ToolCallParam)', 'JAVA_BEAN', 'generateFileTool', 'com.zw.agent.tools.commonTool.GenerateFileTool', 'callAsync', '{\"type\": \"object\", \"required\": [\"fileName\", \"content\"], \"properties\": {\"content\": {\"type\": \"string\", \"description\": \"文件内容\"}, \"fileName\": {\"type\": \"string\", \"description\": \"文件名\"}}, \"additionalProperties\": false}', NULL, NULL, '8f3f1a01ee0d2848be1ea97eec40aed36f01e951d4f9289ebfd46ec012ad033a', 1, 1, 0, 'HIGH', 1, NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-03 21:53:29', '2026-08-03 21:53:29', 0, 0, 2076319328517918111);
INSERT INTO `ai_tool_info_config` VALUES (2084276468633210882, 0, NULL, 'http_delete', 'http_delete', 'Send HTTP DELETE request', 'com.zw.agent.tools.testCustomer.HttpDeleteTool#callAsync(io.agentscope.core.tool.ToolCallParam)', 'JAVA_BEAN', 'httpDeleteTool', 'com.zw.agent.tools.testCustomer.HttpDeleteTool', 'callAsync', '{\"type\": \"object\", \"required\": [\"url\"], \"properties\": {\"url\": {\"type\": \"string\", \"description\": \"Request URL\"}, \"headers\": {\"type\": \"string\", \"description\": \"Request headers in JSON format\"}}, \"additionalProperties\": false}', NULL, NULL, '2988eeb33046041535c2c4b7269a7610124eaf784e57cc3198eba8fa9a541dee', 0, 1, 0, 'HIGH', 1, NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-03 21:53:29', '2026-08-03 21:53:29', 0, 0, 2076319328517918111);
INSERT INTO `ai_tool_info_config` VALUES (2084276468633210883, 0, NULL, 'http_get', 'http_get', 'Send HTTP GET request to specified URL', 'com.zw.agent.tools.testCustomer.HttpGetTool#callAsync(io.agentscope.core.tool.ToolCallParam)', 'JAVA_BEAN', 'httpGetTool', 'com.zw.agent.tools.testCustomer.HttpGetTool', 'callAsync', '{\"type\": \"object\", \"required\": [\"url\"], \"properties\": {\"url\": {\"type\": \"string\", \"description\": \"Request URL\"}, \"headers\": {\"type\": \"string\", \"description\": \"Request headers in JSON format\"}}, \"additionalProperties\": false}', NULL, NULL, '7f6996dfa3319504a38e53b3f1876231d28c820494f54149da846b4b581c2a9d', 0, 1, 0, 'HIGH', 1, NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-03 21:53:29', '2026-08-03 21:53:29', 0, 0, 2076319328517918111);
INSERT INTO `ai_tool_info_config` VALUES (2084276468633210884, 0, NULL, 'http_post', 'http_post', 'Send HTTP POST request with JSON body', 'com.zw.agent.tools.testCustomer.HttpPostTool#callAsync(io.agentscope.core.tool.ToolCallParam)', 'JAVA_BEAN', 'httpPostTool', 'com.zw.agent.tools.testCustomer.HttpPostTool', 'callAsync', '{\"type\": \"object\", \"required\": [\"url\"], \"properties\": {\"url\": {\"type\": \"string\", \"description\": \"Request URL\"}, \"body\": {\"type\": \"string\", \"description\": \"Request body in JSON format\"}, \"headers\": {\"type\": \"string\", \"description\": \"Request headers in JSON format\"}}, \"additionalProperties\": false}', NULL, NULL, '991f826ec989cf5cf4465555240bcf3199bfe724b5e5e15edcfb93115fa18b75', 0, 1, 0, 'HIGH', 1, NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-03 21:53:29', '2026-08-03 21:53:29', 0, 0, 2076319328517918111);
INSERT INTO `ai_tool_info_config` VALUES (2084276468633210885, 0, NULL, 'http_put', 'http_put', 'Send HTTP PUT request with JSON body', 'com.zw.agent.tools.testCustomer.HttpPutTool#callAsync(io.agentscope.core.tool.ToolCallParam)', 'JAVA_BEAN', 'httpPutTool', 'com.zw.agent.tools.testCustomer.HttpPutTool', 'callAsync', '{\"type\": \"object\", \"required\": [\"url\"], \"properties\": {\"url\": {\"type\": \"string\", \"description\": \"Request URL\"}, \"body\": {\"type\": \"string\", \"description\": \"Request body in JSON format\"}, \"headers\": {\"type\": \"string\", \"description\": \"Request headers in JSON format\"}}, \"additionalProperties\": false}', NULL, NULL, '97ca13d9fc4d2aaae05aff5fb778b29cfae18b3c79f2431099e42f8f8fd8139d', 0, 1, 0, 'HIGH', 1, NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-03 21:53:29', '2026-08-03 21:53:29', 0, 0, 2076319328517918111);
INSERT INTO `ai_tool_info_config` VALUES (2084276468633210886, 0, NULL, 'query_order', 'query_order', '查询当前租户下的订单信息', 'com.zw.agent.tools.testCustomer.QueryOrderTool#callAsync(io.agentscope.core.tool.ToolCallParam)', 'JAVA_BEAN', 'queryOrderTool', 'com.zw.agent.tools.testCustomer.QueryOrderTool', 'callAsync', '{\"type\": \"object\", \"required\": [\"orderNo\"], \"properties\": {\"orderNo\": {\"type\": \"string\", \"description\": \"订单号\"}}, \"additionalProperties\": false}', NULL, NULL, 'd26b144ba74a34212e831adab221b05f5eebf725b221d3387f17d8cf083f957c', 1, 1, 0, 'HIGH', 1, NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-03 21:53:29', '2026-08-03 21:53:29', 0, 0, 2076319328517918111);
INSERT INTO `ai_tool_info_config` VALUES (2084276468633210887, 0, NULL, 'query_refund', 'query_refund', '查询当前租户下的退款信息', 'com.zw.agent.tools.testCustomer.QueryRefundTool#callAsync(io.agentscope.core.tool.ToolCallParam)', 'JAVA_BEAN', 'queryRefundTool', 'com.zw.agent.tools.testCustomer.QueryRefundTool', 'callAsync', '{\"type\": \"object\", \"required\": [\"refundNo\"], \"properties\": {\"refundNo\": {\"type\": \"string\", \"description\": \"退款单号\"}}, \"additionalProperties\": false}', NULL, 'order:list', 'b0518366f0df6e80afc58cd823455dd851ee90ecb2e971a2e5dd32beedd1a658', 1, 1, 0, 'HIGH', 1, NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-03 21:53:29', '2026-08-03 21:53:29', 0, 0, 2076319328517918111);
INSERT INTO `ai_tool_info_config` VALUES (2084276468633210888, 0, NULL, 'get_current_time', 'get_current_time', 'Returns the current time in a given IANA timezone.', 'com.zw.agent.tools.testCustomer.SimpleTools#callAsync(io.agentscope.core.tool.ToolCallParam)', 'JAVA_BEAN', 'simpleTools', 'com.zw.agent.tools.testCustomer.SimpleTools', 'callAsync', '{\"type\": \"object\", \"required\": [\"timezone\"], \"properties\": {\"timezone\": {\"type\": \"string\", \"description\": \"IANA timezone, e.g. Asia/Shanghai\"}}, \"additionalProperties\": false}', NULL, NULL, '985c16dfa603a8440c3f0fb220515e3f511c750f4fa2be600d60ce101a6905c5', 1, 1, 0, 'HIGH', 1, NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-03 21:53:29', '2026-08-03 21:53:29', 0, 0, 2076319328517918111);

-- ----------------------------
-- Table structure for ai_tool_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_role_permission`;
CREATE TABLE `ai_tool_role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限规则主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID，用于多租户隔离',
  `tool_id` bigint NOT NULL COMMENT '工具id',
  `role_id` bigint NOT NULL COMMENT '角色id',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'role_code',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称，必须和AgentScope Tool名称一致',
  `rule_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '规则内容，传给Tool.matchRule使用；为空表示匹配该工具所有调用',
  `behavior` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则行为：ALLOW允许，DENY拒绝，ASK询问，PASSTHROUGH交给后续规则',
  `source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'userSettings' COMMENT '规则来源：userSettings/projectSettings/session/suggested/admin',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规则说明，给管理员查看',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2084075684659798018 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent权限规则表：定义某个工具在不同调用模式下允许、拒绝或询问' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_tool_role_permission
-- ----------------------------

-- ----------------------------
-- Table structure for ai_tool_tenant_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_tenant_config`;
CREATE TABLE `ai_tool_tenant_config`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具编码，对应 tool_config.tool_name',
  `group_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户侧工具组id，可覆盖 tool_config.default_group_code',
  `permission_code` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务权限编码，例如 order:query',
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_tool`(`tenant_id` ASC, `tool_name` ASC) USING BTREE,
  INDEX `idx_tenant_enabled`(`tenant_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_tool_code`(`tool_name` ASC) USING BTREE,
  INDEX `idx_group_code`(`group_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户工具授权配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_tool_tenant_config
-- ----------------------------

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `perm_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码（唯一，如 user:add、user:list）',
  `perm_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称（如 用户新增、用户列表）',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父权限ID（用于菜单/按钮层级，0表示顶级）',
  `type` tinyint NOT NULL COMMENT '类型：1-菜单，2-按钮/接口',
  `sort` int NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_perm_code`(`perm_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '权限表（菜单/接口）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` int NOT NULL COMMENT '角色编码（唯一，如 ADMIN、USER）',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称（如 管理员、普通用户）',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2084074021291106307 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (2083125940601069569, 1000, 'admin', '系统管理员', 1, 2076319328517918222, '2026-07-31 17:41:42', '2026-07-31 17:41:42', 0, 0, 2076319328517918222, 2076319328517918111);

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户主键ID',
  `tenant_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户唯一编码，用于接口、Nacos namespace、日志隔离',
  `tenant_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '租户状态：1启用，0停用',
  `nacos_namespace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户对应的 Nacos 命名空间ID，用于 Agent/Skill 隔离',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0,
  `deleted` int NULL DEFAULT 0,
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_code`(`tenant_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2083125452480552962 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户表：平台多租户隔离的根表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (2076319328517918111, 'parent_tenant', '主租户', 1, '', '', '2026-07-31 17:39:45', 2076319328517918222, '2026-07-31 17:39:54', 0, 0, 2076319328517918222);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL,
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tenant_id` bigint NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0,
  `deleted` int NULL DEFAULT 0,
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (2083125633015980034, 'zhiran', 'zhiran', 2076319328517918111, 1, '2026-07-31 17:40:28', 2076319328517918222, '2026-07-31 17:41:52', 0, 0, 2076319328517918222);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  `role_id` bigint NULL DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '所属租户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本',
  `deleted` int NULL DEFAULT 0 COMMENT '0未删, 1删除',
  `update_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (2083125985639505921, 2083125633015980034, 2083125940601069569, 2076319328517918111, 2076319328517918222, '2026-07-31 17:41:52', '2026-07-31 17:41:52', 0, 0, 2076319328517918222);

SET FOREIGN_KEY_CHECKS = 1;
