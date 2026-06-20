package com.zw.agent.mapper;

import com.zw.agent.entity.AiLlmCredentialEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 大模型凭证表：保存每个租户自己的模型供应商鉴权信息 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiLlmCredentialMapper extends BaseMapper<AiLlmCredentialEntity> {

}
