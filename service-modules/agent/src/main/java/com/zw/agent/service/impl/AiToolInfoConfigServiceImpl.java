package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.mapper.AiToolInfoConfigMapper;
import com.zw.agent.service.AiToolInfoConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 全局工具配置表 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@Service
@RequiredArgsConstructor
public class AiToolInfoConfigServiceImpl extends ServiceImpl<AiToolInfoConfigMapper, AiToolInfoConfigEntity> implements AiToolInfoConfigService {

    private final AiToolInfoConfigMapper aiToolInfoConfigMapper;


    @Override
    public int upsertBatch(List<AiToolInfoConfigEntity> list) {
        return aiToolInfoConfigMapper.upsertBatch(list);
    }
}
