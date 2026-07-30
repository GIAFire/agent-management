package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zw.agent.knowledge.dto.KnowledgeBaseCreateRequest;
import com.zw.agent.knowledge.dto.KnowledgeBaseOptionResponse;
import com.zw.agent.knowledge.dto.KnowledgeBaseResponse;
import com.zw.agent.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.zw.agent.knowledge.dto.KnowledgeTaskResponse;
import com.zw.agent.knowledge.service.KnowledgeManagementService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledgeBases")
@RequiredArgsConstructor
public class AiKnowledgeBaseController {

    private final KnowledgeManagementService managementService;

    @GetMapping
    public Result<IPage<KnowledgeBaseResponse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Byte status
    ) {
        return Result.ok(
                managementService.pageKnowledgeBases(
                        current,
                        size,
                        keyword,
                        status
                )
        );
    }

    @PostMapping
    public Result<KnowledgeBaseResponse> create(
            @RequestBody KnowledgeBaseCreateRequest request
    ) {
        return Result.ok(managementService.createKnowledgeBase(request));
    }

    @GetMapping("/options")
    public Result<List<KnowledgeBaseOptionResponse>> options() {
        return Result.ok(managementService.listKnowledgeBaseOptions());
    }

    @GetMapping("/{knowledgeBaseId}")
    public Result<KnowledgeBaseResponse> get(
            @PathVariable Long knowledgeBaseId
    ) {
        return Result.ok(
                managementService.getKnowledgeBase(knowledgeBaseId)
        );
    }

    @PutMapping("/{knowledgeBaseId}")
    public Result<KnowledgeBaseResponse> update(
            @PathVariable Long knowledgeBaseId,
            @RequestBody KnowledgeBaseUpdateRequest request
    ) {
        return Result.ok(
                managementService.updateKnowledgeBase(
                        knowledgeBaseId,
                        request
                )
        );
    }

    @DeleteMapping("/{knowledgeBaseId}")
    public Result<KnowledgeTaskResponse> delete(
            @PathVariable Long knowledgeBaseId
    ) {
        return Result.ok(
                managementService.deleteKnowledgeBase(knowledgeBaseId)
        );
    }
}
