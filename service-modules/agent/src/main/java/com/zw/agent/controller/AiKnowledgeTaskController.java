package com.zw.agent.controller;

import com.zw.agent.knowledge.dto.KnowledgeTaskResponse;
import com.zw.agent.knowledge.dto.KnowledgeFailureResponse;
import com.zw.agent.knowledge.service.KnowledgeManagementService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledgeTasks")
@RequiredArgsConstructor
public class AiKnowledgeTaskController {

    private final KnowledgeManagementService managementService;

    @GetMapping("/{taskId}")
    public Result<KnowledgeTaskResponse> get(@PathVariable Long taskId) {
        return Result.ok(managementService.getTask(taskId));
    }

    @GetMapping("/recent-failures")
    public Result<List<KnowledgeFailureResponse>> recentFailures(
            @RequestParam(defaultValue = "5") int size
    ) {
        return Result.ok(managementService.recentFailures(size));
    }

    @PostMapping("/{taskId}/resubmit")
    public Result<KnowledgeTaskResponse> resubmit(@PathVariable Long taskId) {
        return Result.ok(managementService.resubmitTask(taskId));
    }
}
