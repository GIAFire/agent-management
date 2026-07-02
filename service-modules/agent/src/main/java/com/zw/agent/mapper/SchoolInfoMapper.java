package com.zw.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zw.agent.entity.SchoolInfoEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 学校信息表 Mapper 接口
 * </p>
 *
 * @author 智纬智能体平台
 * @since 2026-06-26
 */
@Mapper
public interface SchoolInfoMapper extends BaseMapper<SchoolInfoEntity> {

    List<Map<String, Object>> findSchoolInfo(Map<String, Object>  params);

    List<Map<String, Object>> queryTableList();

    List<Map<String, Object>> schoolGroupByDistrict();

    List<Map<String, Object>> schoolGroupByType();
}
