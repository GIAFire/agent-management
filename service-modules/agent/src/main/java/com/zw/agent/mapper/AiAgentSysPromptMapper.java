package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentSysPromptEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-12
 */
@Mapper
public interface AiAgentSysPromptMapper extends BaseMapper<AiAgentSysPromptEntity> {

}
