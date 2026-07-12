package com.zw.agent.tools.testCustomer;

import com.zw.agent.service.CommonService;
import com.zw.agent.service.IServiceSchoolInfoService;
import com.zw.agent.tools.ToolResponse;
import com.zw.agent.tools.ToolSchemaUtils;
import com.zw.agent.tools.applicationRunner.Tenant;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tenant("1")
@Slf4j
@Component
public class QuerySchoolInfoTool extends ToolBase {

    @Autowired
    private IServiceSchoolInfoService schoolInfoService;

    public QuerySchoolInfoTool() {
        super(ToolBase.builder()
                .name("query_school_info")
                .description("查询学校信息,如果出现地名+区名,则将区名去掉,提供地名即可,如:龙岗区改为龙岗\n" +
                        "问题示例：1. 查一下龙岗区各街道学校统计情况。" +
                        "如果查询学校名,则只提供学校名专有名词即可,如小学、中学、学校这些词不要作为参数提供,如龙岭初级中学,改为龙岭" +
                        "问题示例: 2. 查一下龙岭初级中学信息。")
                .inputSchema(inputSchema())
                .readOnly(true)
                .concurrencySafe(true));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            List<Map<String, Object>> schoolInfo = schoolInfoService.findSchoolInfo(param.getInput());
            int count = schoolInfo.size();

            ObjectMapper mapper = new ObjectMapper();
            ToolResponse toolResponse = new ToolResponse();
            toolResponse.setData(schoolInfo);
            toolResponse.setCount(count);
            return Mono.just(ToolResultBlock.text(mapper.writeValueAsString(toolResponse)));
        } catch (Exception ex) {
            return Mono.just(ToolResultBlock.error(ex.getMessage()));
        }
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("district", ToolSchemaUtils.stringProperty("区域名"));
        properties.put("schoolAddress", ToolSchemaUtils.stringProperty("街道地址"));
        properties.put("schoolName", ToolSchemaUtils.stringProperty("学校名"));
        properties.put("schoolType", ToolSchemaUtils.stringProperty("学校类型"));
        // list中是必填参数
        return ToolSchemaUtils.objectSchema(properties, List.of());
    }
}
