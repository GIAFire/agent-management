package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiKnowledgeChunkEntity;
import com.zhiran.agent.mapper.AiKnowledgeChunkMapper;
import com.zhiran.agent.service.AiKnowledgeChunkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 知识切片表：记录文档切片内容、向量ID及引用元信息 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Service
public class AiKnowledgeChunkServiceImpl extends ServiceImpl<AiKnowledgeChunkMapper, AiKnowledgeChunkEntity> implements AiKnowledgeChunkService {

}
