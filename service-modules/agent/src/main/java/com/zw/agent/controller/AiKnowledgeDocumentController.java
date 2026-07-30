package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zw.agent.knowledge.dto.KnowledgeDocumentResponse;
import com.zw.agent.knowledge.dto.KnowledgeIndexTaskRequest;
import com.zw.agent.knowledge.dto.KnowledgeTaskResponse;
import com.zw.agent.knowledge.service.KnowledgeManagementService;
import com.zw.common.entity.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AiKnowledgeDocumentController {

    private final KnowledgeManagementService managementService;

    @GetMapping("/knowledgeBases/{knowledgeBaseId}/documents")
    public Result<IPage<KnowledgeDocumentResponse>> page(
            @PathVariable Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String parseStatus
    ) {
        return Result.ok(
                managementService.pageDocuments(
                        knowledgeBaseId,
                        current,
                        size,
                        keyword,
                        parseStatus
                )
        );
    }

    @PostMapping(
            value = "/knowledgeBases/{knowledgeBaseId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Result<KnowledgeDocumentResponse> upload(
            @PathVariable Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file
    ) {
        return Result.ok(
                managementService.uploadDocument(knowledgeBaseId, file)
        );
    }

    @DeleteMapping("/knowledgeDocuments/{documentId}")
    public Result<KnowledgeTaskResponse> delete(
            @PathVariable Long documentId
    ) {
        return Result.ok(managementService.deleteDocument(documentId));
    }

    @PostMapping("/knowledgeDocuments/{documentId}/indexTasks")
    public Result<KnowledgeTaskResponse> createIndexTask(
            @PathVariable Long documentId,
            @RequestBody KnowledgeIndexTaskRequest request
    ) {
        return Result.ok(
                managementService.createIndexTask(documentId, request)
        );
    }

    @GetMapping("/knowledgeDocuments/{documentId}/tasks")
    public Result<IPage<KnowledgeTaskResponse>> tasks(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(
                managementService.pageDocumentTasks(
                        documentId,
                        current,
                        size
                )
        );
    }
}
