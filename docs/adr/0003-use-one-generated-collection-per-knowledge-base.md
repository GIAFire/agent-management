# 每个知识库使用独立且自动命名的 collection

每个知识库使用一个独立的 Milvus collection，以支持知识库级 Embedding 模型和向量维度；collection 名称由后端按租户与知识库 ID 自动生成，前端不允许填写或修改，从而避免跨租户重名、非法名称和存储实现细节泄漏。创建知识库时同步创建并验证 collection，Milvus 连接、维度或配置校验失败时终止创建，不保留不可用的知识库。
