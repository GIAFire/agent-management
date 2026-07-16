package com.zw.agent.service;

import com.zw.agent.entity.AiSkillInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本 服务类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
public interface AiSkillInfoService extends IService<AiSkillInfoEntity> {

    AiSkillInfoEntity createSkillPackage(AiSkillInfoEntity entity);

    AiSkillInfoEntity updateSkillPackage(AiSkillInfoEntity entity);
}
