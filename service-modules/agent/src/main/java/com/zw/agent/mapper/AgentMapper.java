package com.zw.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zw.agent.entity.AgentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 *
 * @author weijianbo
 * @date 2026-02-04
 */
@Mapper
public interface AgentMapper extends BaseMapper<AgentEntity> {
}
