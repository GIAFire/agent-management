package com.zw.agent.factory.ragFactory.enumeration;

public enum BackendStoreType {

    SELF_MANAGED,   // 第三方 embedding + 自己的向量库
    EXTERNAL_RAG    // RAGFlow/Dify/百炼这类完整RAG服务

}
