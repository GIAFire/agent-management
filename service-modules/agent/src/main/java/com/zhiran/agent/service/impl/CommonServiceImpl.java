package com.zhiran.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiran.agent.entity.CommonEntity;
import com.zhiran.agent.mapper.CommonMapper;
import com.zhiran.agent.service.CommonService;
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
