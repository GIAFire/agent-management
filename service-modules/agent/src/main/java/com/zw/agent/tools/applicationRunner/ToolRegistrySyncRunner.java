package com.zw.agent.tools.applicationRunner;

import cn.hutool.crypto.digest.DigestUtil;
import com.zw.agent.service.AiToolInfoConfigService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolRegistrySyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistrySyncRunner.class);

    private final ApplicationContext applicationContext;
    private final AiToolInfoConfigService toolInfoConfigService;


    @Override
    public void run(ApplicationArguments args) {
        // 扫描所有com.zw.agent.tools包下的类

        // 获取所有带@tool注解的方法元信息,元信息包括: @tool注解信息,方法信息,参数信息,返回值信息,还有@permission注解的值

        // 将类全限定名 + 方法名 + 参数类型列表,组合成一个唯一字符串,用于判断数据库中是否存在该工具

        // 将@tool中的name、description、readOnly、concurrencySafe、stateInjected、dangerousFiles、dangerousDirectories、converter这些硬编码阶段中可能会修改的字段使用DigestUtil.sha256Hex()函数生成签名,用于判断该工具内容是否发生变化

        // 使用批量upsert语句: 将这些信息存入数据库,可直接调用该方法:int count = toolInfoConfigService.upsertBatch(list);

    }
}
