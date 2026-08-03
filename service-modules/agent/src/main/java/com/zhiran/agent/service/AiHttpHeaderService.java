package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiHttpHeaderEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhiran.agent.constant.enumeration.HeaderSourceType;

import java.util.List;

/**
 * <p>
 * HTTP请求头配置表 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-26
 */
public interface AiHttpHeaderService extends IService<AiHttpHeaderEntity> {

    List<AiHttpHeaderEntity> getHeaderList(
            Long sourceId,
            Long tenantId,
            HeaderSourceType source
    );
}
