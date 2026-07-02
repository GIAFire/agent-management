package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.entity.SchoolInfoEntity;
import com.zw.agent.mapper.SchoolInfoMapper;
import com.zw.agent.service.IServiceSchoolInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 学校信息表 服务实现类
 * </p>
 *
 * @author 智纬智能体平台
 * @since 2026-06-26
 */
@Service
public class SchoolInfoServiceImpl extends ServiceImpl<SchoolInfoMapper, SchoolInfoEntity> implements IServiceSchoolInfoService {

    @Autowired
    private SchoolInfoMapper schoolMapper;
    @Override
    public List<Map<String, Object>> findSchoolInfo(Map<String, Object>  params) {
        return schoolMapper.findSchoolInfo(params);
    }

    @Override
    public List<Map<String, Object>> queryTableList() {
        return schoolMapper.queryTableList();
    }

    @Override
    public List<Map<String, Object>> schoolGroupByDistrict() {
        return schoolMapper.schoolGroupByDistrict();
    }

    @Override
    public List<Map<String, Object>> schoolGroupByType() {
        return schoolMapper.schoolGroupByType();
    }
}
