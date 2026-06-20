package com.zw.agent.mapper;

import com.zw.agent.entity.AiModelConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 模型配置表：把凭证、模型名、生成参数组合成可被 Agent 选择的模型 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfigEntity> {

}
