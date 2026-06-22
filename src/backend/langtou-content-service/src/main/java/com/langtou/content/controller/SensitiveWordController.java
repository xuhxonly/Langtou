package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.Result;
import com.langtou.content.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词内部接口（供其他服务通过Feign调用）
 */
@RestController
@RequestMapping("/api/v1/sensitive-words")
@RequiredArgsConstructor
@Tag(name = "敏感词服务", description = "敏感词接口")
    public class SensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    /**
     * 检查文本是否包含敏感词
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> checkSensitiveWord(@RequestBody Map<String, String> params) {
        String text = params.get("text");
        boolean contains = sensitiveWordService.containsSensitiveWord(text);
        Map<String, Object> result = new HashMap<>();
        result.put("containsSensitiveWord", contains);
        return Result.success(result);
    }
}
