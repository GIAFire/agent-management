package com.zw.agent.tools.commonTool;

import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.service.AiAgentWorkspaceFileService;
import com.zw.agent.tools.ToolSchemaUtils;
import com.zw.agent.tools.applicationRunner.Tenant;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tenant("2076319328517918111")
@Slf4j
@Component
public class GenerateFileTool extends ToolBase {

    @Autowired
    private AiAgentWorkspaceFileService workspaceFileService;

    public GenerateFileTool() {
        super(ToolBase.builder()
                .name("generate_file")
                .description("写文件、生成文件、生成报告")
                .inputSchema(inputSchema())
                .readOnly(true)
                .concurrencySafe(true));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        RuntimeContext runtimeContext = param.getRuntimeContext();
        Map<String, Object> input = param.getInput();

        String fileName = String.valueOf(input.get("fileName"));
        String content = String.valueOf(input.get("content"));
        String toolCallId = param.getToolUseBlock().getId();

        AiAgentWorkspaceFileEntity file = workspaceFileService.saveGeneratedFile(
                runtimeContext,
                toolCallId,
                fileName,
                content
        );

        String resultJson = """
                {
                  "message": "文件已保存,请将下载链接提供给用户: http://localhost:8100/download/%s",
                  "filePath": "文件已保存,请记住文件路径,下次的回答可能会用到: %s",
                  "fileName": "%s",
                  "fileType": "%s"
                }
                """.formatted(
                file.getId(),
                file.getRelativePath(),
                file.getFileName(),
                file.getFileExt()
        );

        return Mono.just(ToolResultBlock.builder()
//                .id(param.getToolUseBlock().getId())
//                .name(getName())
                .output(List.of(TextBlock.builder().text(resultJson).build()))
                .build());
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("fileName", ToolSchemaUtils.stringProperty("文件名"));
        properties.put("content", ToolSchemaUtils.stringProperty("文件内容"));
        return ToolSchemaUtils.objectSchema(properties, List.of("fileName","content"));
    }
}
