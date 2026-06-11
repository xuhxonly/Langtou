package com.langtou.common.client;

import com.langtou.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "langtou-user-service", path = "/api/v1")
public interface UserClient {

    @GetMapping("/users/{userId}")
    Result<Map<String, Object>> getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/users/me")
    Result<Map<String, Object>> getCurrentUser(@RequestHeader("userId") Long userId);

    /**
     * 批量获取用户信息
     */
    @GetMapping("/users/batch")
    Result<List<Map<String, Object>>> batchGetUsers(@RequestParam("userIds") List<Long> userIds);

    /**
     * 获取用户关注的所有用户ID
     */
    @GetMapping("/users/{userId}/following/ids")
    Result<List<Long>> getFollowingIds(@PathVariable("userId") Long userId);

    /**
     * 搜索用户（按昵称或用户名模糊匹配）
     */
    @GetMapping("/users/search")
    Result<List<Map<String, Object>>> searchUsers(@RequestParam("keyword") String keyword,
                                                   @RequestParam("limit") int limit);
}
