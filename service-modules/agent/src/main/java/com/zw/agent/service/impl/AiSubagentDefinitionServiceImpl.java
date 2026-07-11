package com.zw.agent.service.impl;

import com.zw.agent.entity.AiSubagentDefinitionEntity;
import com.zw.agent.mapper.AiSubagentDefinitionMapper;
import com.zw.agent.service.AiSubagentDefinitionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Service
public class AiSubagentDefinitionServiceImpl extends ServiceImpl<AiSubagentDefinitionMapper, AiSubagentDefinitionEntity> implements AiSubagentDefinitionService {

}
