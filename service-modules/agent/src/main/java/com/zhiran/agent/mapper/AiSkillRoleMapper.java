package com.zhiran.agent.mapper;

import com.zhiran.agent.entity.AiSkillRoleEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Skill角色权限表：可配置skill角色权限 Mapper 接口
 * </p>
 *
 * @author 智伟
 * @since 2026-07-20
 */
@Mapper
public interface AiSkillRoleMapper extends BaseMapper<AiSkillRoleEntity> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM ai_skill_role WHERE skill_info_id = #{skillId} AND tenant_id = #{tenantId}")
    int hardDeleteBySkill(
            @Param("skillId") Long skillId,
            @Param("tenantId") Long tenantId
    );
}
