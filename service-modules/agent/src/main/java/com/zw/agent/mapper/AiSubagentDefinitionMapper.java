package com.zw.agent.mapper;

import com.zw.agent.entity.AiSubagentDefinitionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Mapper
public interface AiSubagentDefinitionMapper extends BaseMapper<AiSubagentDefinitionEntity> {

}
