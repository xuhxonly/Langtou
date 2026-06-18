package com.langtou.content.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.Draft;
import com.langtou.content.service.DraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "草稿箱", description = "用户草稿箱管理")
@RestController
@RequestMapping("/api/v1/users/me/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;

    @Operation(summary = "保存草稿")
    @PostMapping
    public Result<Draft> saveDraft(@RequestBody Draft draft,
                                    @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        Draft result = draftService.saveDraft(userId, draft);
        return Result.success("保存成功", result);
    }

    @Operation(summary = "更新草稿")
    @PutMapping("/{draftId}")
    public Result<Draft> updateDraft(@PathVariable Long draftId,
                                      @RequestBody Draft draft,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        Draft result = draftService.updateDraft(userId, draftId, draft);
        return Result.success("更新成功", result);
    }

    @Operation(summary = "草稿列表")
    @GetMapping
    public Result<PageResult<Draft>> getDraftList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        PageResult<Draft> result = draftService.getDraftList(userId, page, size);
        return Result.success(result);
    }

    @Operation(summary = "草稿详情")
    @GetMapping("/{draftId}")
    public Result<Draft> getDraftDetail(@PathVariable Long draftId,
                                         @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        Draft result = draftService.getDraftDetail(userId, draftId);
        return Result.success(result);
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/{draftId}")
    public Result<Void> deleteDraft(@PathVariable Long draftId,
                                     @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        draftService.deleteDraft(userId, draftId);
        return Result.success("删除成功");
    }
}
