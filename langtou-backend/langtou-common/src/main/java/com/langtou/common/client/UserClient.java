package com.langtou.common.client;

import com.langtou.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@FeignClient(name = "langtou-user-service", path = "/api/v1", fallbackFactory = UserClient.UserClientFallbackFactory.class)
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

    @Slf4j
    @Component
    class UserClientFallbackFactory implements FallbackFactory<UserClient> {
        @Override
        public UserClient create(Throwable cause) {
            return new UserClient() {
                @Override
                public Result<Map<String, Object>> getUserById(Long userId) {
                    log.error("UserClient.getUserById 调用失败, userId={}", userId, cause);
                    return Result.error("用户服务不可用");
                }

                @Override
                public Result<Map<String, Object>> getCurrentUser(Long userId) {
                    log.error("UserClient.getCurrentUser 调用失败, userId={}", userId, cause);
                    return Result.error("用户服务不可用");
                }

                @Override
                public Result<List<Map<String, Object>>> batchGetUsers(List<Long> userIds) {
                    log.error("UserClient.batchGetUsers 调用失败, userIds={}", userIds, cause);
                    return Result.error("用户服务不可用");
                }

                @Override
                public Result<List<Long>> getFollowingIds(Long userId) {
                    log.error("UserClient.getFollowingIds 调用失败, userId={}", userId, cause);
                    return Result.error("用户服务不可用");
                }

                @Override
                public Result<List<Map<String, Object>>> searchUsers(String keyword, int limit) {
                    log.error("UserClient.searchUsers 调用失败, keyword={}", keyword, cause);
                    return Result.success(Collections.emptyList());
                }
            };
        }
    }
}
