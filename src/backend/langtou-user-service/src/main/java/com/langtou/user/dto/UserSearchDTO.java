package com.langtou.user.dto;

import lombok.Data;

@Data
public class UserSearchDTO {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;
}
