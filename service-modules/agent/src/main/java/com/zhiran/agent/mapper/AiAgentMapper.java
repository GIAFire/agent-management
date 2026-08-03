package com.zhiran.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zhiran.agent.entity.AiAgentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiAgentMapper extends BaseMapper<AiAgentEntity> {

    @InterceptorIgnore(tenantLine = "true")
    AgentConfigDTO getAgentConfigById(@Param("agentId") Long agentId,
                                      @Param("tenantId") Long tenantId,
                                      @Param("agentConfigId") Long agentConfigId);

    List<AiAgentEntity> subAgentList(@Param("agentId") Long agentId, @Param("tenantId") Long tenantId);
}
