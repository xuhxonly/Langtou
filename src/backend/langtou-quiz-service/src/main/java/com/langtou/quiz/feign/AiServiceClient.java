package com.langtou.quiz.feign;

import com.langtou.common.result.Result;
import com.langtou.quiz.feign.dto.AiGenerateRequest;
import com.langtou.quiz.feign.dto.AiGenerateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "langtou-ai-service", path = "/api/v1/ai", fallbackFactory = AiServiceClient.AiServiceClientFallbackFactory.class)
public interface AiServiceClient {

    @PostMapping("/quiz/generate")
    Result<AiGenerateResponse> generateQuiz(@RequestBody AiGenerateRequest request);

    @Slf4j
    @Component
    class AiServiceClientFallbackFactory implements FallbackFactory<AiServiceClient> {
        @Override
        public AiServiceClient create(Throwable cause) {
            log.error("AiServiceClient 调用失败", cause);
            return request -> Result.error("AI 服务不可用，请稍后重试");
        }
    }
}
