package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent 版本表：保存每次可视化配置发布后的不可变快照 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiAgentConfigMapper extends BaseMapper<AiAgentConfigEntity> {

}
