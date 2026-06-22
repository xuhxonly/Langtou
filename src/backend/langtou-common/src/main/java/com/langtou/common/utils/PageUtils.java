package com.langtou.common.utils;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public class PageUtils {

    public static <T> Page<T> buildPage(long current, long size) {
        return new Page<>(current, size);
    }

    public static <T> Page<T> buildPage(long current, long size, String orderBy, boolean isAsc) {
        Page<T> page = new Page<>(current, size);
        if (orderBy != null && !orderBy.isEmpty()) {
            if (isAsc) {
                page.addOrder(OrderItem.asc(orderBy));
            } else {
                page.addOrder(OrderItem.desc(orderBy));
            }
        }
        return page;
    }
}
