package com.langtou.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 埋点事件DTO
 * 接收客户端上报的用户行为事件
 */
@Data
public class AnalyticsEventDTO {

    /** 事件唯一ID */
    @NotBlank(message = "事件ID不能为空")
    private String eventId;

    /** 事件名称: page_view, note_impression, note_click, note_like, note_collect, note_share, note_comment, search, publish, follow */
    @NotBlank(message = "事件名称不能为空")
    private String eventName;

    /** 用户ID */
    private String userId;

    /** 事件时间戳（毫秒） */
    private Long timestamp;

    /** 动态属性 */
    private Map<String, Object> properties;

    /**
     * 批量上报请求体
     */
    @Data
    public static class BatchRequest {

        @JsonProperty("events")
        private java.util.List<AnalyticsEventDTO> events;
    }
}
