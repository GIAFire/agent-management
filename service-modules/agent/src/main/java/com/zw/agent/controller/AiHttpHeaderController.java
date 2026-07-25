package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiHttpHeaderEntity;
import com.zw.agent.service.AiHttpHeaderService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * HTTP请求头配置表 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-07-26
 */
@AllArgsConstructor
@RestController
@RequestMapping("/httpHeader")
public class AiHttpHeaderController {

    private final AiHttpHeaderService httpHeaderService;

    @GetMapping("/list")
    public Result<List<AiHttpHeaderEntity>> list() {
        return Result.ok(httpHeaderService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiHttpHeaderEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(httpHeaderService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiHttpHeaderEntity> getById(@PathVariable Long id) {
        return Result.ok(httpHeaderService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiHttpHeaderEntity entity) {
        return Result.ok(httpHeaderService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiHttpHeaderEntity entity) {
        return Result.ok(httpHeaderService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(httpHeaderService.removeById(id));
    }


}
