package com.zw.agent.service.impl;

import com.zw.agent.entity.AiLlmCredentialEntity;
import com.zw.agent.mapper.AiLlmCredentialMapper;
import com.zw.agent.service.AiLlmCredentialService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 大模型凭证表：保存每个租户自己的模型供应商鉴权信息 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiLlmCredentialServiceImpl extends ServiceImpl<AiLlmCredentialMapper, AiLlmCredentialEntity> implements AiLlmCredentialService {

}
