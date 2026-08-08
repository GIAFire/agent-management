package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiSubagentInstanceEntity;
import com.zhiran.agent.mapper.AiSubagentInstanceMapper;
import com.zhiran.agent.service.AiSubagentInstanceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 子Agent运行实例表：记录父Agent创建的子Agent实例、会话、工作区和暴露状态 服务实现类
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-11
 */
@Service
public class AiSubagentInstanceServiceImpl extends ServiceImpl<AiSubagentInstanceMapper, AiSubagentInstanceEntity> implements AiSubagentInstanceService {

}
