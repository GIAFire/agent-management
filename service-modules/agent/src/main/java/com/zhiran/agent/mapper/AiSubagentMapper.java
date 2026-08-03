package com.zhiran.agent.mapper;

import com.zhiran.agent.entity.AiSubagentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiran.agent.entity.DTO.SubagentHeaderDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Collection;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Mapper
public interface AiSubagentMapper extends BaseMapper<AiSubagentEntity> {

    List<SubagentHeaderDTO> subAgentList(@Param("agentId") Long agentId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM ai_subagent WHERE id = #{id} AND tenant_id = #{tenantId}")
    int hardDeleteById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "<script>",
            "SELECT id, subagent_name FROM ai_subagent",
            "WHERE tenant_id = #{tenantId}",
            "AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<AiSubagentEntity> selectNamesIncludingDeleted(
            @Param("tenantId") Long tenantId,
            @Param("ids") Collection<Long> ids
    );
}
