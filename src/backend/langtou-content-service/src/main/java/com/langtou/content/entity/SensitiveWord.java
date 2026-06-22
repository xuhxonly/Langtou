package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词实体
 */
@Data
@TableName("sensitive_word")
public class SensitiveWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 敏感词内容
     */
    private String word;

    /**
     * 分类：crime-违法犯罪 porn-色情低俗 gamble-赌博 drug-毒品 politics-政治敏感
     */
    private String category;

    /**
     * 来源：BUILT_IN-内置 CUSTOM-自定义
     */
    private String source;

    /**
     * 状态：ENABLED-启用 DISABLED-禁用
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
