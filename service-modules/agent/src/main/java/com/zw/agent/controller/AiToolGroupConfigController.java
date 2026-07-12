package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiToolGroupConfigEntity;
import com.zw.agent.mapper.AiToolGroupConfigMapper;
import com.zw.agent.service.AiToolGroupConfigService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 工具组配置表 前端控制器
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@RestController
@RequestMapping("/toolGroupConfig")
@AllArgsConstructor
public class AiToolGroupConfigController {

    private final AiToolGroupConfigService aiToolGroupConfigService;
    private final AiToolGroupConfigMapper aiToolGroupConfigMapper;

    /**
     * 查询所有工具组配置列表
     */
    @GetMapping("/list")
    public Result<List<AiToolGroupConfigEntity>> list() {
        return Result.ok(aiToolGroupConfigMapper.selectGroups());
    }

    /**
     * 分页查询工具组配置
     */
    @GetMapping("/page")
    public Result<IPage<AiToolGroupConfigEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        List<AiToolGroupConfigEntity> groups = aiToolGroupConfigMapper.selectGroups();
        Page<AiToolGroupConfigEntity> page = new Page<>(current, size);
        int total = groups.size();
        int fromIndex = (int) Math.min(Math.max((current - 1) * size, 0), total);
        int toIndex = (int) Math.min(fromIndex + size, total);
        page.setTotal(total);
        page.setRecords(fromIndex >= toIndex ? Collections.emptyList() : groups.subList(fromIndex, toIndex));
        return Result.ok(page);
    }

    /**
     * 根据ID查询工具组配置
     */
    @GetMapping("/{id}")
    public Result<AiToolGroupConfigEntity> getById(@PathVariable Long id) {
        return Result.ok(aiToolGroupConfigMapper.selectGroupById(id));
    }

    /**
     * 新增工具组配置
     */
    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiToolGroupConfigEntity entity) {
        return Result.ok(aiToolGroupConfigMapper.insertGroup(EntityDefaults.create(entity)) > 0);
    }

    /**
     * 更新工具组配置
     */
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiToolGroupConfigEntity entity) {
        return Result.ok(aiToolGroupConfigMapper.updateGroupById(EntityDefaults.update(entity)) > 0);
    }

    /**
     * 根据ID删除工具组配置
     */
    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiToolGroupConfigService.removeById(id));
    }

}
