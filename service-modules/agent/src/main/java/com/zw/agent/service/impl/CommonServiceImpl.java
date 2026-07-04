package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.entity.CommonEntity;
import com.zw.agent.mapper.CommonMapper;
import com.zw.agent.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CommonServiceImpl extends ServiceImpl<CommonMapper, CommonEntity> implements CommonService {

    @Autowired
    private CommonMapper commonMapper;
    @Override
    public List<Map<String, Object>> testQueryEquipmentInfo(Map<String, Object> params) {
        return commonMapper.testQueryEquipmentInfo(params);
    }

    @Override
    public List<Map<String, Object>> bigData(Map<String, Object> params) {
        return commonMapper.bigData(params);
    }

    @Override
    public long count() {
        return commonMapper.count();
    }
}
