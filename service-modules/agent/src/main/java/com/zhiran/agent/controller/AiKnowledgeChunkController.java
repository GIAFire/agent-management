package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhiran.agent.knowledge.dto.KnowledgeChunkResponse;
import com.zhiran.agent.knowledge.service.KnowledgeManagementService;
import com.zhiran.common.entity.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledgeDocuments")
@RequiredArgsConstructor
public class AiKnowledgeChunkController {

    private final KnowledgeManagementService managementService;

    @GetMapping("/{documentId}/chunks")
    public Result<IPage<KnowledgeChunkResponse>> page(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(
                managementService.pageChunks(documentId, current, size)
        );
    }
}
