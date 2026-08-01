# 预建 MySQL 会话状态表

MySQL 会话状态复用应用主数据源，并固定写入 `ai_state_store` 表；该表由部署方预先创建，`StateStoreFactory` 使用 `createIfNotExist=false` 只做存在性校验，不在运行时执行 DDL。相比采用 AgentScope 默认表和自动建表，这一选择保留了数据库最小权限、变更审计与发布控制，但后续调整表名或存储位置时必须显式迁移既有会话状态。
