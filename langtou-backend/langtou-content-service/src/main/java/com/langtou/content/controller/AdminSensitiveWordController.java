package com.langtou.content.controller;

import com.langtou.common.annotation.RequireRole;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.SensitiveWord;
import com.langtou.content.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "敏感词管理", description = "后台敏感词管理")
@RestController
@RequestMapping("/api/v1/admin/sensitive-words")
@RequiredArgsConstructor
public class AdminSensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    @Operation(summary = "分页查询敏感词列表")
    @GetMapping
    @RequireRole("ADMIN")
    public Result<PageResult<SensitiveWord>> getSensitiveWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String word,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status) {
        PageResult<SensitiveWord> result = sensitiveWordService.getSensitiveWordPage(page, size, word, category, source, status);
        return Result.success(result);
    }

    @Operation(summary = "添加敏感词")
    @PostMapping
    @RequireRole("ADMIN")
    public Result<SensitiveWord> addSensitiveWord(@RequestBody Map<String, String> params) {
        String word = params.get("word");
        String category = params.get("category");
        String source = params.get("source");
        SensitiveWord sensitiveWord = sensitiveWordService.addSensitiveWord(word, category, source);
        return Result.success("添加成功", sensitiveWord);
    }

    @Operation(summary = "启用/禁用敏感词")
    @PutMapping("/{id}/status")
    @RequireRole("ADMIN")
    public Result<Void> updateStatus(@PathVariable Long id,
                                      @RequestBody Map<String, String> params) {
        String status = params.get("status");
        sensitiveWordService.updateSensitiveWordStatus(id, status);
        return Result.success("状态更新成功", null);
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        sensitiveWordService.deleteSensitiveWord(id);
        return Result.success("删除成功", null);
    }
}
