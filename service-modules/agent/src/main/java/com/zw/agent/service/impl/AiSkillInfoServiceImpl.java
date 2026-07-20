package com.zw.agent.service.impl;

import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.DTO.SkillFileDTO;
import com.zw.agent.mapper.AiSkillInfoMapper;
import com.zw.agent.service.AiSkillInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
@RequiredArgsConstructor
public class AiSkillInfoServiceImpl extends ServiceImpl<AiSkillInfoMapper, AiSkillInfoEntity> implements AiSkillInfoService {

    private final AiSkillInfoMapper skillInfoMapper;

    @Override
    public SkillFileDTO getAgentSkill(String name, Long agentId) {
        return skillInfoMapper.getAgentSkill(name, agentId);
    }

    @Override
    public List<SkillFileDTO> getAgentSkillName(Long agentId) {
        return skillInfoMapper.getAgentSkillName(agentId);
    }
}
