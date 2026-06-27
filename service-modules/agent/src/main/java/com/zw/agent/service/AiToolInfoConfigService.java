package com.zw.agent.service;

import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 全局工具配置表 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
public interface AiToolInfoConfigService extends IService<AiToolInfoConfigEntity> {

    int upsertBatch(List<AiToolInfoConfigEntity> list);

}
