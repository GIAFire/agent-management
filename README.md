# AgentScope 项目架构文档

## 项目概述

AgentScope 是一个基于 Spring Cloud 微服务架构的 AI Agent 管理平台，提供智能体创建、配置、对话等功能。项目采用前后端分离架构，后端使用 Spring Cloud Alibaba 技术栈，前端使用 Vue 3 + Element Plus。

## 技术栈

### 后端技术栈
- **Spring Boot**: 4.0.6
- **Spring Cloud**: 2025.1.0
- **Spring Cloud Alibaba**: 2025.1.0.0
- **Java**: 21
- **数据库**: MySQL 8.3.0
- **ORM**: MyBatis Plus 3.5.15
- **缓存**: Redis
- **注册中心**: Nacos
- **流量控制**: Sentinel
- **服务调用**: OpenFeign
- **工具库**: Hutool 5.8.46

### 前端技术栈
- **Vue**: 3.5.32
- **构建工具**: Vite 8.0.8
- **UI框架**: Element Plus 2.11.5
- **状态管理**: Pinia 2.3.1
- **路由**: Vue Router 4.6.3
- **HTTP客户端**: Axios 1.13.1
- **Markdown渲染**: markdown-it 14.2.0
- **Node.js**: ^20.19.0 || >=22.12.0

## 项目目录结构

```
agentScope/
├── auth/                          # 认证授权模块
├── common/                        # 公共模块
│   ├── common-core/              # 核心公共模块
│   ├── common-log/               # 日志公共模块
│   ├── common-redis/             # Redis公共模块
│   └── common-security/          # 安全公共模块
├── gateway/                       # API网关模块
├── service-modules/               # 业务服务模块
│   └── agent/                    # Agent业务服务
├── ui/                           # 前端应用
├── docs/                         # 文档目录
│   └── sql/                      # SQL脚本
├── agent/                        # Agent配置目录（IDE配置）
├── pom.xml                       # Maven父POM
└── .gitignore                    # Git忽略配置
```

## 模块详细说明

### 1. auth - 认证授权中心

**功能描述**: 提供用户认证、授权、JWT令牌管理等功能。

**核心功能**:
- 用户登录认证
- JWT令牌生成与验证
- 用户信息管理
- 多租户支持

**主要类**:
- `AuthController`: 认证控制器，处理登录请求
- `SysUserController`: 用户管理控制器
- `SysUserService`: 用户服务接口
- `JwtProperties`: JWT配置属性
- `AuthExceptionHandler`: 认证异常处理器

**依赖**:
- common-redis: Redis缓存支持
- common-log: 日志记录
- common-core: 核心工具
- common-security: 安全上下文

**端口**: 配置在 application.yml 中

---

### 2. gateway - API网关

**功能描述**: 统一API入口，提供路由转发、JWT认证、流量控制等功能。

**核心功能**:
- 路由转发
- JWT令牌验证
- 用户信息传递
- 白名单路径配置

**主要类**:
- `JwtAuthGlobalFilter`: JWT认证全局过滤器
- `GatewayAuthProperties`: 网关认证配置
- `JwtProperties`: JWT配置属性
- `GatewayApplication`: 网关启动类

**特性**:
- 支持路径白名单配置
- OPTIONS请求直接放行
- 解析JWT后将用户信息放入请求头传递给下游服务

---

### 3. common - 公共模块

#### 3.1 common-core - 核心公共模块

**功能描述**: 提供项目核心工具类、常量定义、基础实体等。

**主要类**:
- `Result`: 统一返回结果封装
- `BaseEntity`: 基础实体类
- `HttpStatus`: HTTP状态码常量
- `RedisConstants`: Redis键常量
- `UserHeaderConstants`: 用户请求头常量
- `JwtUtils`: JWT工具类
- `AESUtil`: AES加密工具
- `JacksonConfig`: Jackson配置
- `MybatisPlusConfig`: MyBatis Plus配置

#### 3.2 common-redis - Redis公共模块

**功能描述**: 提供Redis缓存操作封装。

**主要类**:
- `RedisService`: Redis服务接口，提供缓存操作方法
- `RedisConfig`: Redis配置类

#### 3.3 common-log - 日志公共模块

**功能描述**: 提供API日志记录、请求追踪等功能。

**主要类**:
- `ApiLogFilter`: API日志过滤器
- `RequestMdcFilter`: 请求MDC过滤器，用于日志追踪

#### 3.4 common-security - 安全公共模块

**功能描述**: 提供用户上下文管理、安全拦截等功能。

**主要类**:
- `UserContext`: 用户上下文工具类
- `UserInfo`: 用户信息实体
- `UserContextInterceptor`: 用户上下文拦截器
- `UserContextAutoConfiguration`: 自动配置类

---

### 4. service-modules/agent - Agent业务服务

**功能描述**: AI Agent核心业务服务，提供智能体管理、配置、对话等功能。

**端口**: 8100  
**上下文路径**: /agent  
**服务名**: agent

**核心功能模块**:

#### 4.1 智能体管理
- `AiAgentController`: 智能体基础信息管理
- `AiAgentConfigController`: 智能体配置管理
- `AiAgentSessionController`: 智能体会话管理
- `AiAgentMessageController`: 智能体消息管理
- `AiAgentRunController`: 智能体运行管理
- `AiAgentRunEventController`: 智能体运行事件管理

#### 4.2 对话功能
- `AgentChatController`: 智能体对话控制器
  - 支持流式对话 (chatStream)
  - 支持阻塞式对话 (chatBlock)
  - 使用 Server-Sent Events (SSE) 实现流式响应

#### 4.3 配置管理
- `AiModelConfigController`: AI模型配置管理
- `AiToolGroupConfigController`: 工具分组配置管理
- `AiToolInfoConfigController`: 工具信息配置管理

#### 4.4 租户管理
- `SysTenantController`: 租户管理控制器

#### 4.5 运行时
- `AgentRuntimeFactory`: Agent运行时工厂
- `AgentFullConfigService`: Agent完整配置服务
- `AgentConfigQueryService`: Agent配置查询服务
- `AgentRuntimeConfig`: Agent运行时配置

**实体类**:
- `AiAgentEntity`: 智能体实体
- `AiAgentConfigEntity`: 智能体配置实体
- `AiAgentSessionEntity`: 智能体会话实体
- `AiAgentMessageEntity`: 智能体消息实体
- `AiAgentRunEntity`: 智能体运行实体
- `AiAgentRunEventEntity`: 智能体运行事件实体
- `AiModelConfigEntity`: AI模型配置实体
- `AiToolGroupConfigEntity`: 工具分组配置实体
- `AiToolInfoConfigEntity`: 工具信息配置实体
- `SysTenantEntity`: 租户实体

**事件与响应**:
- `AgentChatResponse`: Agent对话响应
- `AgentStreamResponse`: Agent流式响应
- `AgentRuntimeEvent`: Agent运行时事件
- `AgentEventType`: Agent事件类型枚举

**配置**:
- Nacos服务发现与配置中心
- 动态数据源支持（主从分离）
- Redis缓存
- MyBatis Plus ORM

---

### 5. ui - 前端应用

**功能描述**: Vue 3 单页应用，提供智能体管理、对话、配置等功能的可视化界面。

**构建工具**: Vite  
**开发命令**: `npm run dev`  
**构建命令**: `npm run build`

**核心功能模块**:

#### 5.1 智能体管理
- **智能体管理** (`/agent/manage`): 智能体列表、创建、编辑
- **智能体对话** (`/agent/chat/:agentId`): 与智能体进行对话交互
- **智能体配置** (`/agent/agent-config`): 配置智能体的各项参数

#### 5.2 资源管理
- **模型管理** (`/agent/model`): AI模型配置管理
- **知识库管理** (`/agent/knowledge`): 知识库配置管理
- **工具管理** (`/agent/tool`): 工具配置管理
- **技能包管理** (`/agent/skill-package`): 技能包管理
- **MCP管理** (`/agent/mcp`): MCP协议管理
- **钩子管理** (`/agent/hook`): 钩子函数管理
- **敏感词管理** (`/agent/sensitive-word`): 敏感词过滤管理

#### 5.3 用户管理
- **租户管理** (`/user/tenant`): 多租户管理
- **用户管理** (`/user/manage`): 用户账号管理

#### 5.4 系统功能
- **登录** (`/login`): 用户登录认证

**技术架构**:
- **路由**: Vue Router，支持路由守卫和权限控制
- **状态管理**: Pinia，管理用户状态等
- **HTTP请求**: Axios，封装了统一的请求配置
- **UI组件**: Element Plus，提供丰富的UI组件
- **布局**: 自定义布局组件，包含导航栏、侧边栏、标签页等

**目录结构**:
```
ui/src/
├── axios/              # Axios请求封装
│   ├── agent.js       # Agent相关API
│   ├── agentConfig.js # Agent配置API
│   ├── auth.js        # 认证API
│   ├── chat.js        # 对话API
│   ├── model.js       # 模型API
│   ├── tenant.js      # 租户API
│   └── request.js     # 请求配置
├── components/         # 公共组件
├── layout/            # 布局组件
│   └── components/    # 布局子组件
│       ├── AppMain.vue
│       ├── Navbar.vue
│       ├── Sidebar/
│       └── TagsView.vue
├── router/            # 路由配置
│   └── modules/
│       └── admin.js   # 管理后台路由
├── stores/            # Pinia状态管理
│   └── user.js       # 用户状态
├── utils/             # 工具函数
│   ├── auth.js       # 认证工具
│   └── encryption.js # 加密工具
├── views/             # 页面组件
│   ├── agent/        # 智能体相关页面
│   ├── agentConfig/  # 智能体配置页面
│   ├── hook/         # 钩子管理页面
│   ├── knowledge/    # 知识库页面
│   ├── login/        # 登录页面
│   ├── mcp/          # MCP管理页面
│   ├── model/        # 模型管理页面
│   ├── sensitiveWord/# 敏感词页面
│   ├── skillPackage/ # 技能包页面
│   ├── tenant/       # 租户管理页面
│   ├── tool/         # 工具管理页面
│   └── user/         # 用户管理页面
├── App.vue           # 根组件
└── main.js           # 入口文件
```

---

## 服务调用关系

```
用户请求
    ↓
Gateway (网关)
    ↓ JWT验证、用户信息传递
    ↓
    ├─→ Auth (认证服务) - 用户登录、认证
    │
    └─→ Agent (业务服务) - 智能体管理、对话
            ↓
        使用 Common 模块提供的公共功能
            ├─ common-core: 工具类、常量
            ├─ common-redis: 缓存操作
            ├─ common-log: 日志记录
            └─ common-security: 安全上下文
```

---

## 数据库设计

项目使用 MySQL 数据库，支持主从分离配置。主要数据表包括：

### Agent 相关表
- `ai_agent`: 智能体基础信息表
- `ai_agent_config`: 智能体配置表
- `ai_agent_session`: 智能体会话表
- `ai_agent_message`: 智能体消息表
- `ai_agent_run`: 智能体运行记录表
- `ai_agent_run_event`: 智能体运行事件表

### 配置相关表
- `ai_model_config`: AI模型配置表
- `ai_tool_group_config`: 工具分组配置表
- `ai_tool_info_config`: 工具信息配置表

### 系统相关表
- `sys_user`: 系统用户表
- `sys_tenant`: 租户表

---

## 配置说明

### Nacos 配置
- **服务发现**: 所有服务注册到 Nacos
- **配置中心**: 使用 Nacos 管理应用配置
- **命名空间**: 使用命名空间隔离不同环境

### Redis 配置
- 用于缓存用户信息、会话数据等
- 支持分布式场景下的数据共享

### 数据源配置
- 支持动态数据源切换
- 支持主从分离
- 使用 Druid 连接池

### MyBatis Plus 配置
- 全局主键策略: ASSIGN_ID
- 逻辑删除支持
- 自动填充功能

---

## 部署架构

### 开发环境
- Nacos: localhost:8848
- Redis: localhost:6379
- MySQL: 配置在环境变量中

### 服务端口
- Gateway: 配置在 application.yml
- Auth: 配置在 application.yml
- Agent: 8100

---

## 开发指南

### 后端开发
1. 确保 Java 21 已安装
2. 启动 Nacos、Redis、MySQL
3. 导入项目到 IDE
4. 运行各服务的 Application 类

### 前端开发
1. 确保 Node.js 20.19+ 或 22.12+ 已安装
2. 进入 ui 目录
3. 安装依赖: `npm install`
4. 启动开发服务器: `npm run dev`
5. 构建生产版本: `npm run build`

---

## 安全特性

- JWT 令牌认证
- 多租户数据隔离
- 用户上下文传递
- 敏感词过滤
- 路径白名单配置

---

## 扩展功能

- 知识库管理
- 工具管理
- 技能包管理
- MCP 协议支持
- 钩子函数机制
- 敏感词过滤

---

## 总结

AgentScope 是一个功能完善的 AI Agent 管理平台，采用现代化的微服务架构，具备良好的扩展性和可维护性。项目支持多租户、流式对话、丰富的配置管理等功能，适合构建企业级的 AI 应用平台。
