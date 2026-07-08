package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentPlanEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent计划表：保存Plan Mode生成的计划元数据、内容快照、审批和执行状态 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@Mapper
public interface AiAgentPlanMapper extends BaseMapper<AiAgentPlanEntity> {

}
