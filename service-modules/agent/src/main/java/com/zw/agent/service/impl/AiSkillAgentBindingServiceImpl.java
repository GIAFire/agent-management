package com.zw.agent.service.impl;

import com.zw.agent.entity.AiSkillAgentBindingEntity;
import com.zw.agent.mapper.AiSkillAgentBindingMapper;
import com.zw.agent.service.AiSkillAgentBindingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent与Skill绑定表：定义某个Agent配置版本安装哪些Skill以及安装作用域 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
public class AiSkillAgentBindingServiceImpl extends ServiceImpl<AiSkillAgentBindingMapper, AiSkillAgentBindingEntity> implements AiSkillAgentBindingService {

}
