package com.langtou.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

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

    @Data
    public static class PageResult<T> {
        private List<T> list;
        private Long total;
        private Long current;
        private Long size;
        private Long pages;

        public PageResult(List<T> list, Long total, Long current, Long size, Long pages) {
            this.list = list;
            this.total = total;
            this.current = current;
            this.size = size;
            this.pages = pages;
        }

        public static <T> PageResult<T> of(IPage<T> page) {
            return new PageResult<>(
                    page.getRecords(),
                    page.getTotal(),
                    page.getCurrent(),
                    page.getSize(),
                    page.getPages()
            );
        }
    }
}
