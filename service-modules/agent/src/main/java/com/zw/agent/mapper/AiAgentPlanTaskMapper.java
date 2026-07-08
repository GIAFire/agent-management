package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentPlanTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent计划任务表：保存todo_write生成的结构化任务清单和执行状态 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@Mapper
public interface AiAgentPlanTaskMapper extends BaseMapper<AiAgentPlanTaskEntity> {

}
