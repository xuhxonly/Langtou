package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.entity.Product;

import java.util.List;

public interface ProductService {

    /**
     * 创建商品
     */
    Product createProduct(Product product, Long creatorId);

    /**
     * 更新商品
     */
    Product updateProduct(Long productId, Long creatorId, Product product);

    /**
     * 获取商品详情
     */
    Product getProductById(Long productId);

    /**
     * 我的商品列表（按创作者）
     */
    PageResult<Product> getMyProducts(Long creatorId, String status, int page, int size);

    /**
     * 按分类查询商品列表
     */
    PageResult<Product> getProductsByCategory(String category, int page, int size);

    /**
     * 上架/下架商品
     */
    Product toggleProductStatus(Long productId, Long creatorId);

    /**
     * 笔记关联商品（一个笔记最多关联5个商品）
     */
    void linkProductsToNote(Long noteId, Long creatorId, List<Long> productIds);

    /**
     * 获取笔记关联的商品列表
     */
    List<Product> getNoteProducts(Long noteId);

    /**
     * 记录商品点击
     */
    void recordProductClick(Long productId, Long noteId);

    /**
     * 获取创作者商品橱窗（用户端）
     */
    PageResult<Product> getCreatorShowcase(Long creatorId, int page, int size);
}
