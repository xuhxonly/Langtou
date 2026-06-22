package com.langtou.quiz.feign;

import com.langtou.common.result.Result;
import com.langtou.quiz.feign.dto.AiCreativeRequest;
import com.langtou.quiz.feign.dto.AiCreativeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "langtou-ai-service", path = "/api/v1/ai/creative",
        fallbackFactory = AiCreativeClient.AiCreativeClientFallbackFactory.class)
public interface AiCreativeClient {

    @PostMapping("/generate")
    Result<AiCreativeResponse> generateCreative(@RequestBody AiCreativeRequest request);

    @Slf4j
    @Component
    class AiCreativeClientFallbackFactory implements FallbackFactory<AiCreativeClient> {
        @Override
        public AiCreativeClient create(Throwable cause) {
            log.error("AiCreativeClient 调用失败", cause);
            return request -> Result.error("AI 创意服务不可用，请稍后重试");
        }
    }
}
