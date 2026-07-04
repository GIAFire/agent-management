package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentStateOpLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * AgentState操作日志表：记录状态加载、保存、压缩、清理等操作 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@Mapper
public interface AiAgentStateOpLogMapper extends BaseMapper<AiAgentStateOpLogEntity> {

}
