package com.zw.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.SchoolInfoEntity;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 学校信息表 服务类
 * </p>
 *
 * @author 智纬智能体平台
 * @since 2026-06-26
 */
public interface IServiceSchoolInfoService extends IService<SchoolInfoEntity> {

    List<Map<String, Object>> findSchoolInfo(Map<String, Object>  params);

    List<Map<String, Object>> queryTableList();

    List<Map<String, Object>> schoolGroupByDistrict();

    List<Map<String, Object>> schoolGroupByType();
}
