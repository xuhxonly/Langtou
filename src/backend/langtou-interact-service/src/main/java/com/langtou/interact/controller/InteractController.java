package com.langtou.interact.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.common.result.PageResult;
import com.langtou.interact.dto.CommentCreateDTO;
import com.langtou.interact.dto.CommentVO;
import com.langtou.interact.dto.ReportCreateDTO;
import com.langtou.interact.entity.Comment;
import com.langtou.interact.entity.Report;
import com.langtou.interact.entity.ShareRecord;
import com.langtou.interact.service.InteractService;
import com.langtou.interact.service.ReportService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "浜掑姩鏈嶅姟", description = "鐐硅禐銆佽瘎璁恒€佽浆鍙戙€佷妇鎶ャ€佹敹钘忕瓑浜掑姩鐩稿叧鎺ュ彛")
public class InteractController {

    private final InteractService interactService;
    private final ReportService reportService;
    @PostMapping("/notes/{noteId}/like")
    @Operation(summary = "鐐硅禐绗旇", description = "瀵规寚瀹氱瑪璁扮偣璧?)
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> like(
            @Parameter(name = "noteId", description = "绗旇ID", required = true) @PathVariable Long noteId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.like(userId, noteId);
        return Result.success("鐐硅禐鎴愬姛");
    }

    @DeleteMapping("/notes/{noteId}/like")
    @Operation(summary = "鍙栨秷鐐硅禐", description = "鍙栨秷瀵规寚瀹氱瑪璁扮殑鐐硅禐")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> unlike(
            @Parameter(name = "noteId", description = "绗旇ID", required = true) @PathVariable Long noteId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.unlike(userId, noteId);
        return Result.success("鍙栨秷鐐硅禐鎴愬姛");
    }

    @GetMapping("/notes/{noteId}/comments")
    @Operation(summary = "鑾峰彇璇勮鍒楄〃", description = "鑾峰彇鎸囧畾绗旇鐨勮瘎璁哄垪琛紙鏍戝舰缁撴瀯锛?)
    public Result<PageResult<CommentVO>> getComments(
            @Parameter(name = "noteId", description = "绗旇ID", required = true) @PathVariable Long noteId,
            @Parameter(name = "current", description = "褰撳墠椤?) @RequestParam(defaultValue = "1") long current,
            @Parameter(name = "size", description = "姣忛〉鏁伴噺") @RequestParam(defaultValue = "10") long size,
            @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        Page<CommentVO> page = interactService.getCommentsWithTree(noteId, userId, current, size);
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/notes/{noteId}/comments")
    @Operation(summary = "鍙戣〃璇勮", description = "瀵规寚瀹氱瑪璁板彂琛ㄨ瘎璁烘垨鍥炲")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Comment> comment(
            @Parameter(name = "noteId", description = "绗旇ID", required = true) @PathVariable Long noteId,
            @RequestBody Map<String, Object> params,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        String content = params.get("content").toString();
        Long parentId = params.containsKey("parentId") && params.get("parentId") != null
                ? Long.valueOf(params.get("parentId").toString()) : null;
        Comment comment = interactService.comment(userId, noteId, content, parentId);
        return Result.success("璇勮鎴愬姛", comment);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "鍒犻櫎璇勮", description = "鍒犻櫎鎸囧畾璇勮")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> deleteComment(
            @Parameter(name = "commentId", description = "璇勮ID", required = true) @PathVariable Long commentId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.deleteComment(userId, commentId);
        return Result.success("鍒犻櫎鎴愬姛");
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "鐐硅禐璇勮", description = "瀵规寚瀹氳瘎璁虹偣璧?)
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> likeComment(
            @Parameter(name = "commentId", description = "璇勮ID", required = true) @PathVariable Long commentId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.likeComment(userId, commentId);
        return Result.success("鐐硅禐璇勮鎴愬姛");
    }

    @PostMapping("/notes/{noteId}/share")
    @Operation(summary = "杞彂绗旇", description = "杞彂鎸囧畾绗旇锛屾敮鎸佸绉嶅垎浜被鍨?)
    @SecurityRequirement(name = "bearer-jwt")
    public Result<ShareRecord> share(
            @Parameter(name = "noteId", description = "绗旇ID", required = true) @PathVariable Long noteId,
            @RequestBody Map<String, String> params,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        String shareType = params.get("shareType");
        ShareRecord record = interactService.share(userId, noteId, shareType);
        return Result.success("杞彂鎴愬姛", record);
    }

    @GetMapping("/notes/{noteId}/share-link")
    @Operation(summary = "鐢熸垚鍒嗕韩閾炬帴", description = "鐢熸垚鎸囧畾绗旇鐨勫垎浜摼鎺?)
    public Result<Map<String, String>> getShareLink(
            @Parameter(name = "noteId", description = "绗旇ID", required = true) @PathVariable Long noteId) {
        String shareLink = interactService.generateShareLink(noteId);
        Map<String, String> data = new HashMap<>();
        data.put("shareLink", shareLink);
        return Result.success(data);
    }

    @PostMapping("/notes/{noteId}/report")
    @Operation(summary = "涓炬姤绗旇", description = "瀵硅繚瑙勭瑪璁拌繘琛屼妇鎶?)
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Report> reportNote(
            @Parameter(name = "noteId", description = "绗旇ID", required = true) @PathVariable Long noteId,
            @Valid @RequestBody ReportCreateDTO dto,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        Report report = reportService.createReport(userId, noteId, dto);
        return Result.success("涓炬姤鎴愬姛", report);
    }
}
