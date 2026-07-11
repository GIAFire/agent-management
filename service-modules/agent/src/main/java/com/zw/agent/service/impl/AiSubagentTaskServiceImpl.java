package com.zw.agent.service.impl;

import com.zw.agent.entity.AiSubagentTaskEntity;
import com.zw.agent.mapper.AiSubagentTaskMapper;
import com.zw.agent.service.AiSubagentTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 子Agent任务表：记录agent_spawn/agent_send产生的同步或后台委派任务 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Service
public class AiSubagentTaskServiceImpl extends ServiceImpl<AiSubagentTaskMapper, AiSubagentTaskEntity> implements AiSubagentTaskService {

}
