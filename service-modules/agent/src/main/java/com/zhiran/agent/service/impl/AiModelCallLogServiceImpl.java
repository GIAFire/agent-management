package com.zhiran.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiran.agent.entity.AiModelCallLogEntity;
import com.zhiran.agent.mapper.AiModelCallLogMapper;
import com.zhiran.agent.service.AiModelCallLogService;
import org.springframework.stereotype.Service;

@Service
public class AiModelCallLogServiceImpl
        extends ServiceImpl<AiModelCallLogMapper, AiModelCallLogEntity>
        implements AiModelCallLogService {
}
