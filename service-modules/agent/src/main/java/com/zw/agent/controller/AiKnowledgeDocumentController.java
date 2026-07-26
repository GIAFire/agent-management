package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeDocumentEntity;
import com.zw.agent.service.AiKnowledgeDocumentService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai-knowledge-document-entity")
@RequiredArgsConstructor
public class AiKnowledgeDocumentController {


    private final AiKnowledgeDocumentService aiKnowledgeDocumentService;

    @GetMapping("/list")
    public Result<List<AiKnowledgeDocumentEntity>> list() {
        return Result.ok(aiKnowledgeDocumentService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeDocumentEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeDocumentService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeDocumentEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeDocumentService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeDocumentEntity entity) {
        return Result.ok(aiKnowledgeDocumentService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeDocumentEntity entity) {
        return Result.ok(aiKnowledgeDocumentService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeDocumentService.removeById(id));
    }
}
