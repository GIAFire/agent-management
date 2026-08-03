package com.zhiran.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zhiran.agent.entity.AiSkillInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiran.agent.entity.DTO.SkillFileDTO;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本 Mapper 接口
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Mapper
public interface AiSkillInfoMapper extends BaseMapper<AiSkillInfoEntity> {

    @InterceptorIgnore(tenantLine = "true")
    SkillFileDTO getAgentSkill(
            @Param("skillCode") String skillCode,
            @Param("agentId") Long agentId,
            @Param("agentConfigId") Long agentConfigId,
            @Param("tenantId") Long tenantId,
            @Param("roleCodes") Collection<String> roleCodes
    );

    @InterceptorIgnore(tenantLine = "true")
    List<SkillFileDTO> getAgentSkillName(
            @Param("agentId") Long agentId,
            @Param("agentConfigId") Long agentConfigId,
            @Param("tenantId") Long tenantId,
            @Param("roleCodes") Collection<String> roleCodes
    );

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM ai_skill_info WHERE tenant_id = #{tenantId} AND LOWER(source) = LOWER(#{skillCode})")
    long countCodeIncludingDeleted(
            @Param("tenantId") Long tenantId,
            @Param("skillCode") String skillCode
    );

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM ai_skill_info WHERE id = #{id} AND tenant_id = #{tenantId}")
    int hardDeleteById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "<script>",
            "SELECT id, name, source FROM ai_skill_info",
            "WHERE tenant_id = #{tenantId}",
            "AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<AiSkillInfoEntity> selectNamesIncludingDeleted(
            @Param("tenantId") Long tenantId,
            @Param("ids") Collection<Long> ids
    );
}
