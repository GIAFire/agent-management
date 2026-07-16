package com.zw.agent.service.impl;

import com.zw.agent.entity.AiSkillLogEntity;
import com.zw.agent.mapper.AiSkillLogMapper;
import com.zw.agent.service.AiSkillLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Skill使用日志表：记录Agent读取、加载、执行Skill的行为 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
public class AiSkillLogServiceImpl extends ServiceImpl<AiSkillLogMapper, AiSkillLogEntity> implements AiSkillLogService {

}
