package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内容相似度实体
 */
@Data
@TableName("content_similarities")
public class ContentSimilarity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 内容A的ID
     */
    private Long contentIdA;

    /**
     * 内容B的ID
     */
    private Long contentIdB;

    /**
     * 相似度得分(0.0000~1.0000)
     */
    private BigDecimal similarityScore;

    /**
     * 检测方式: TEXT_HASH/IMAGE_HASH/AI
     */
    private String checkMethod;

    /**
     * 检测时间
     */
    private LocalDateTime createdAt;
}
