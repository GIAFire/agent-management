package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentToolEntity;
import com.zw.agent.mapper.AiAgentToolMapper;
import com.zw.agent.service.AiAgentToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent与Tool绑定表：定义某个Agent启用了哪些工具 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-12
 */
@Service
public class AiAgentToolServiceImpl extends ServiceImpl<AiAgentToolMapper, AiAgentToolEntity> implements AiAgentToolService {

}
