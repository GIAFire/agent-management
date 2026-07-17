package com.zw.agent.config;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import io.agentscope.core.nacos.skill.NacosSkillRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(NacosSkillProperties.class)
public class NacosSkillConfiguration {

    /**
     * 创建 Nacos AI Registry 客户端。
     *
     * 直接复用 spring.cloud.nacos 中已经配置的：
     * 1. server-addr
     * 2. username
     * 3. password
     */
    @Bean(destroyMethod = "shutdown")
    public AiService nacosAiService(
            @Value("${spring.cloud.nacos.server-addr}")
            String serverAddr,

            @Value("${spring.cloud.nacos.username:}")
            String username,

            @Value("${spring.cloud.nacos.password:}")
            String password,

            @Value("${agent.skill.nacos.namespace-id:public}")
            String namespaceId
    ) throws NacosException {

        Properties properties = new Properties();

        properties.setProperty(
                PropertyKeyConst.SERVER_ADDR,
                serverAddr
        );

        properties.setProperty(
                PropertyKeyConst.NAMESPACE,
                namespaceId
        );

        if (StringUtils.hasText(username)) {
            properties.setProperty(
                    PropertyKeyConst.USERNAME,
                    username
            );
        }

        if (StringUtils.hasText(password)) {
            properties.setProperty(
                    PropertyKeyConst.PASSWORD,
                    password
            );
        }

        return AiFactory.createAiService(properties);
    }

    /**
     * 创建 AgentScope Nacos Skill 仓库。
     */
    @Bean
    public NacosSkillRepository nacosSkillRepository(
            AiService nacosAiService,
            NacosSkillProperties config
    ) {
        Properties properties = new Properties();

        if (StringUtils.hasText(config.getVersion())) {
            properties.setProperty(
                    NacosSkillRepository.SKILL_VERSION_PATH,
                    config.getVersion()
            );
        } else if (
                StringUtils.hasText(config.getLabel())
        ) {
            properties.setProperty(
                    NacosSkillRepository.SKILL_LABEL_PATH,
                    config.getLabel()
            );
        }

        return new WindowsSafeNacosSkillRepository(
                nacosAiService,
                config.getNamespaceId(),
                properties,
                config.getKnownSkillNames()
        );
    }
}