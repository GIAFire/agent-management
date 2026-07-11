package com.zw.agent.tools.testCustomer;

import com.zw.agent.service.CommonService;
import com.zw.agent.tools.ToolResponse;
import com.zw.agent.tools.ToolSchemaUtils;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import lombok.extern.slf4j.Slf4j;
import org.icepear.echarts.Bar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CommonTool extends ToolBase {

    @Autowired
    private CommonService commonService;

    public CommonTool() {
        super(ToolBase.builder()
                .name("bigData")
                .description("查询大数据信息")
                .inputSchema(inputSchema())
                .readOnly(true)
                .concurrencySafe(true));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            List<Map<String, Object>> res = commonService.bigData(param.getInput());
            Integer count = res.size();

            ObjectMapper mapper = new ObjectMapper();
            ToolResponse toolResponse = new ToolResponse();
            toolResponse.setData(res);
            toolResponse.setCount(count);
            return Mono.just(ToolResultBlock.text(mapper.writeValueAsString(toolResponse)));
        } catch (Exception ex) {
            return Mono.just(ToolResultBlock.error(ex.getMessage()));
        }
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        return ToolSchemaUtils.objectSchema(properties, List.of());
    }
}
