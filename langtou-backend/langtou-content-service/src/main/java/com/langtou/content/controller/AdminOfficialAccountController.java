package com.langtou.content.controller;

import com.langtou.common.annotation.RequireRole;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.OfficialAccount;
import com.langtou.content.service.OfficialAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 官方账号管理接口（管理员端）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/official-accounts")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminOfficialAccountController {

    private final OfficialAccountService officialAccountService;

    /**
     * 创建官方账号
     * POST /api/v1/admin/official-accounts
     */
    @PostMapping
    public Result<OfficialAccount> createAccount(@RequestBody OfficialAccount account) {
        OfficialAccount created = officialAccountService.createAccount(account);
        return Result.success(created);
    }

    /**
     * 更新官方账号
     * PUT /api/v1/admin/official-accounts/{id}
     */
    @PutMapping("/{id}")
    public Result<OfficialAccount> updateAccount(@PathVariable Long id, @RequestBody OfficialAccount account) {
        OfficialAccount updated = officialAccountService.updateAccount(id, account);
        return Result.success(updated);
    }

    /**
     * 认证审核
     * PUT /api/v1/admin/official-accounts/{id}/review
     */
    @PutMapping("/{id}/review")
    public Result<OfficialAccount> reviewAccount(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body != null ? body.get("action") : null;
        OfficialAccount reviewed = officialAccountService.reviewAccount(id, action);
        return Result.success(reviewed);
    }

    /**
     * 官方账号列表
     * GET /api/v1/admin/official-accounts
     */
    @GetMapping
    public Result<PageResult<OfficialAccount>> listAccounts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String status) {
        PageResult<OfficialAccount> result = officialAccountService.listAccounts(page, size, accountType, status);
        return Result.success(result);
    }

    /**
     * 官方账号详情
     * GET /api/v1/admin/official-accounts/{id}
     */
    @GetMapping("/{id}")
    public Result<OfficialAccount> getAccountDetail(@PathVariable Long id) {
        OfficialAccount account = officialAccountService.getAccountDetail(id);
        return Result.success(account);
    }
}
