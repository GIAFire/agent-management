package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiLlmCredentialEntity;
import com.zw.agent.service.AiLlmCredentialService;
import com.zw.common.entity.Result;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 大模型凭证表：保存每个租户自己的模型供应商鉴权信息 前端控制器
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RestController
@RequestMapping("/llmCredential")
@AllArgsConstructor
public class AiLlmCredentialController {
    private final AiLlmCredentialService aiLlmCredentialService;

    @GetMapping("/list")
    public Result<List<AiLlmCredentialEntity>> list() {
        return Result.ok(aiLlmCredentialService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiLlmCredentialEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiLlmCredentialService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiLlmCredentialEntity> getById(@PathVariable Long id) {
        return Result.ok(aiLlmCredentialService.getById(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AiLlmCredentialEntity entity) {
        return Result.ok(aiLlmCredentialService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AiLlmCredentialEntity entity) {
        return Result.ok(aiLlmCredentialService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiLlmCredentialService.removeById(id));
    }

}
