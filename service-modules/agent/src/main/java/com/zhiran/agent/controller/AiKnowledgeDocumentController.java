package com.zhiran.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhiran.agent.knowledge.dto.KnowledgeDocumentResponse;
import com.zhiran.agent.knowledge.dto.KnowledgeIndexRequest;
import com.zhiran.agent.knowledge.service.KnowledgeManagementService;
import com.zhiran.common.entity.Result;
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
    public Result<KnowledgeDocumentResponse> delete(
            @PathVariable Long documentId
    ) {
        return Result.ok(managementService.deleteDocument(documentId));
    }

    @PostMapping("/knowledgeDocuments/{documentId}/index")
    public Result<KnowledgeDocumentResponse> submitIndex(
            @PathVariable Long documentId,
            @RequestBody KnowledgeIndexRequest request
    ) {
        return Result.ok(
                managementService.submitIndex(documentId, request)
        );
    }
}
