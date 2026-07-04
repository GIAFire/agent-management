package com.zw.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zw.agent.entity.CommonEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommonMapper extends BaseMapper<CommonEntity>{

    List<Map<String, Object>> testQueryEquipmentInfo(Map<String, Object> params);

    long count();

    List<Map<String, Object>> bigData(Map<String, Object> params);
}
