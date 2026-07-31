package com.zw.agent.mapper;

import com.zw.agent.entity.AiSkillResourceEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源 Mapper 接口
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Mapper
public interface AiSkillResourceMapper extends BaseMapper<AiSkillResourceEntity> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM ai_skill_resource WHERE id = #{id} AND tenant_id = #{tenantId}")
    int hardDeleteById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM ai_skill_resource WHERE skill_id = #{skillId} AND tenant_id = #{tenantId}")
    int hardDeleteBySkill(
            @Param("skillId") Long skillId,
            @Param("tenantId") Long tenantId
    );

    @InterceptorIgnore(tenantLine = "true")
    @Delete("""
            DELETE FROM ai_skill_resource
            WHERE skill_id = #{skillId}
              AND tenant_id = #{tenantId}
              AND (resource_path = #{folderPath} OR resource_path LIKE CONCAT(#{folderPath}, '/%'))
            """)
    int hardDeleteFolder(
            @Param("skillId") Long skillId,
            @Param("tenantId") Long tenantId,
            @Param("folderPath") String folderPath
    );
}
