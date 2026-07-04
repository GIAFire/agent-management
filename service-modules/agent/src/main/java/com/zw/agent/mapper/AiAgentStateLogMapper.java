package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentStateLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * AgentState状态引用表：记录AgentScope运行时状态在外部存储中的位置和元数据 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@Mapper
public interface AiAgentStateLogMapper extends BaseMapper<AiAgentStateLogEntity> {

    Boolean updateByRuntimeKey(AiAgentStateLogEntity update);
}
