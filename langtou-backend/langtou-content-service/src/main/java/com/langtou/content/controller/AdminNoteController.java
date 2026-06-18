package com.langtou.content.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.result.Result;
import com.langtou.content.entity.AuditLog;
import com.langtou.content.entity.Content;
import com.langtou.content.mapper.AuditLogMapper;
import com.langtou.content.mapper.ContentMapper;
import com.langtou.content.service.ContentAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminNoteController {
    private final ContentMapper contentMapper;
    private final ContentAuditService contentAuditService;
    private final AuditLogMapper auditLogMapper;

    @GetMapping("/notes")
    public Result<Page<Content>> getNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Page<Content> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Content::getTitle, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Content::getStatus, "pending".equals(status) ? 0 : 1);
        }
        wrapper.orderByDesc(Content::getCreatedAt);
        return Result.success(contentMapper.selectPage(pageParam, wrapper));
    }

    @PostMapping("/notes/{noteId}/approve")
    public Result<Void> approveNote(@PathVariable Long noteId) {
        Content content = new Content();
        content.setId(noteId);
        content.setStatus(1);
        contentMapper.updateById(content);
        log.info("管理员审核通过笔记: noteId={}", noteId);
        return Result.success("审核通过");
    }

    @PostMapping("/notes/{noteId}/reject")
    public Result<Void> rejectNote(@PathVariable Long noteId, @RequestBody(required = false) Map<String, String> body) {
        Content content = new Content();
        content.setId(noteId);
        content.setStatus(2); // 2=rejected
        contentMapper.updateById(content);
        log.info("管理员拒绝笔记: noteId={}, reason={}", noteId, body != null ? body.get("reason") : "");
        return Result.success("已拒绝");
    }

    @PostMapping("/notes/batch-approve")
    public Result<Void> batchApprove(@RequestBody List<Long> noteIds) {
        noteIds.forEach(id -> {
            Content content = new Content();
            content.setId(id);
            content.setStatus(1);
            contentMapper.updateById(content);
        });
        log.info("批量审核通过: count={}", noteIds.size());
        return Result.success("批量通过成功");
    }

    @PostMapping("/notes/batch-reject")
    public Result<Void> batchReject(@RequestBody List<Long> noteIds) {
        noteIds.forEach(id -> {
            Content content = new Content();
            content.setId(id);
            content.setStatus(2);
            contentMapper.updateById(content);
        });
        log.info("批量拒绝: count={}", noteIds.size());
        return Result.success("批量拒绝成功");
    }

    @DeleteMapping("/notes/{noteId}")
    public Result<Void> deleteNote(@PathVariable Long noteId) {
        contentMapper.deleteById(noteId);
        log.info("管理员删除笔记: noteId={}", noteId);
        return Result.success("删除成功");
    }

    @PostMapping("/notes/{noteId}/offline")
    public Result<Void> offlineNote(@PathVariable Long noteId) {
        Content content = new Content();
        content.setId(noteId);
        content.setStatus(3); // 3=offline
        contentMapper.updateById(content);
        return Result.success("已下线");
    }

    @PostMapping("/notes/{noteId}/restore")
    public Result<Void> restoreNote(@PathVariable Long noteId) {
        Content content = new Content();
        content.setId(noteId);
        content.setStatus(1);
        contentMapper.updateById(content);
        return Result.success("已恢复");
    }

    @GetMapping("/notes/hot")
    public Result<List<Content>> getHotNotes(@RequestParam(defaultValue = "5") int limit) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getStatus, 1).orderByDesc(Content::getLikeCount).last("LIMIT " + limit);
        return Result.success(contentMapper.selectList(wrapper));
    }

    @GetMapping("/sensitive-words")
    public Result<List<String>> getSensitiveWords() {
        return Result.success(contentAuditService.getSensitiveWords());
    }

    @PostMapping("/sensitive-words")
    public Result<Void> addSensitiveWord(@RequestBody Map<String, String> body) {
        String word = body.get("word");
        if (word != null && !word.isEmpty()) {
            contentAuditService.addSensitiveWord(word);
            log.info("管理员添加敏感词: {}", word);
        }
        return Result.success("添加成功");
    }

    @DeleteMapping("/sensitive-words/{wordId}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long wordId) {
        contentAuditService.removeSensitiveWord(wordId);
        return Result.success("删除成功");
    }

    @GetMapping("/audit/log")
    public Result<Page<AuditLog>> getAuditLog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return Result.success(auditLogMapper.selectPage(pageParam, wrapper));
    }
}
