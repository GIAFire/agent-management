package com.zw.agent.tools.testCustomer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zw.agent.service.CommonService;
import com.zw.agent.tools.ToolResponse;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import lombok.extern.slf4j.Slf4j;
import org.icepear.echarts.Bar;
import org.icepear.echarts.Option;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zw.agent.tools.testCustomer.ToolSchemaUtils.optionalString;

@Slf4j
@Component
public class CommonTool extends ToolBase {

    @Autowired
    private CommonService commonService;

    public CommonTool() {
        super(ToolBase.builder()
                .name("test_query_equipment_info")
                .description("查询设备信息")
                .inputSchema(inputSchema())
                .readOnly(true)
                .concurrencySafe(true));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            List<Map<String, Object>> res = commonService.testQueryEquipmentInfo(param.getInput());
            Long count = commonService.count();

            // 1. 创建柱状图实例
            Bar bar = new Bar();
            // 2. 使用 addXAxis 和 addYAxis 方法设置坐标轴
            bar.setLegend() // 设置图例（可选）
                    .setTooltip("item") // 设置提示框（可选）
                    .addXAxis(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) // 设置 X 轴数据
                    .addYAxis() // 添加 Y 轴 (默认 value 类型)
                    .addSeries("bar", new Number[]{120, 200, 150, 80, 70, 110, 130}); // 添加系列数据
            String option = "{xAxis: {type: 'category',data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']}, yAxis: {type: 'value'},series: [{data: [120, 200, 150, 80, 70, 110, 130],type: 'bar'}]}";
            ObjectMapper mapper = new ObjectMapper();
            ToolResponse toolResponse = new ToolResponse();
            toolResponse.setData(res);
            toolResponse.setCount(count);
            toolResponse.setOptionTemplate(option);
            toolResponse.setToLLMMessage("请大模型拿到返回结果后检查`optionTemplate`模板字段是否为空,如果不为空,请结合`data`字段填写`optionTemplate`模板");
            log.warn(option);
            return Mono.just(ToolResultBlock.text(mapper.writeValueAsString(toolResponse)));
        } catch (Exception ex) {
            return Mono.just(ToolResultBlock.error(ex.getMessage()));
        }
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("equipmentName", ToolSchemaUtils.stringProperty("设备名称"));
        properties.put("level", ToolSchemaUtils.stringProperty("设备等级"));
        properties.put("factory", ToolSchemaUtils.stringProperty("生产厂家"));
        properties.put("orgName", ToolSchemaUtils.stringProperty("游乐园名称"));
        properties.put("contactName", ToolSchemaUtils.stringProperty("设备联系人"));
        properties.put("contactJob", ToolSchemaUtils.stringProperty("联系人职位"));
        // list中是必填参数
        return ToolSchemaUtils.objectSchema(properties, null);
    }
}
