package com.zw.agent.mapper;

import com.zw.agent.entity.AiSkillLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Skill使用日志表：记录Agent读取、加载、执行Skill的行为 Mapper 接口
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Mapper
public interface AiSkillLogMapper extends BaseMapper<AiSkillLogEntity> {

}
