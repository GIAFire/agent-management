package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentModelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 模型配置表：把凭证、模型名、生成参数组合成可被 Agent 选择的模型 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiAgentModelMapper extends BaseMapper<AiAgentModelEntity> {

    @Select("""
            SELECT COUNT(*)
            FROM ai_agent_config
            WHERE tenant_id = #{tenantId}
              AND model_id = #{modelId}
            """)
    long countAgentConfigReferences(
            @Param("modelId") Long modelId,
            @Param("tenantId") Long tenantId
    );

    @org.apache.ibatis.annotations.Update("""
            UPDATE ai_agent_config
            SET model_id = NULL,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND model_id = #{modelId}
            """)
    int clearAgentConfigReferences(
            @Param("modelId") Long modelId,
            @Param("tenantId") Long tenantId
    );

    @Delete("DELETE FROM ai_agent_model WHERE id = #{modelId} AND tenant_id = #{tenantId}")
    int hardDeleteById(
            @Param("modelId") Long modelId,
            @Param("tenantId") Long tenantId
    );
}
