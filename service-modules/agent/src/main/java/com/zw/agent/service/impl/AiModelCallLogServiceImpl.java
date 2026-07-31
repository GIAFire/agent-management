package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.entity.AiModelCallLogEntity;
import com.zw.agent.mapper.AiModelCallLogMapper;
import com.zw.agent.service.AiModelCallLogService;
import org.springframework.stereotype.Service;

@Service
public class AiModelCallLogServiceImpl
        extends ServiceImpl<AiModelCallLogMapper, AiModelCallLogEntity>
        implements AiModelCallLogService {
}
