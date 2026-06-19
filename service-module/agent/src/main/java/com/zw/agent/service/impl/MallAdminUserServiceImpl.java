package com.zw.agent.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.entity.MallAdminUser;
import com.zw.agent.mapper.MallAdminUserMapper;
import com.zw.agent.service.MallAdminUserService;
import org.springframework.stereotype.Service;

@Service
public class MallAdminUserServiceImpl  extends ServiceImpl<MallAdminUserMapper, MallAdminUser> implements MallAdminUserService {
}
