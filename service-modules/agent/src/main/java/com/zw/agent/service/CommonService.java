package com.zw.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.CommonEntity;

import java.util.List;
import java.util.Map;

public interface CommonService extends IService<CommonEntity> {

    List<Map<String, Object>> testQueryEquipmentInfo(Map<String, Object> params);

    @Override
    long count();
}
