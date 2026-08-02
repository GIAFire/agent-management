# 通过配置选择全局向量存储

应用通过 `rag.store.type` 在启动配置中选择全局唯一的 Milvus、Elasticsearch、PgVector 或 Qdrant，并由统一工厂路由到隔离后端差异的 Provider；每个知识库在所选后端使用独立物理空间，实际名称为全局 `collection-prefix` 与数据库 `collection_name` 的组合，距离度量统一为 `COSINE`。缺失或无效配置不阻止应用启动，必要地址缺失时不注册知识检索工具，临时连接失败则在实际使用时重试并降级；切换后端需要重启，项目不迁移或兼容旧数据，用户自行清理旧存储并重新切片入库。本决策取代 ADR-0001 和 ADR-0003。
