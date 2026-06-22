package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.Product;
import com.langtou.content.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品橱窗 - 用户端
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/shop")
@RequiredArgsConstructor
@Tag(name = "小店服务", description = "小店接口")
    public class ShopController {

    private final ProductService productService;

    /**
     * 查看创作者商品橱窗
     * GET /api/v1/shop/{creatorId}/products
     */
    @GetMapping("/{creatorId}/products")
    public Result<PageResult<Product>> getCreatorShowcase(@PathVariable Long creatorId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        PageResult<Product> result = productService.getCreatorShowcase(creatorId, page, size);
        return Result.success(result);
    }

    /**
     * 商品详情页
     * GET /api/v1/products/{id}/detail
     */
    @GetMapping("/products/{id}/detail")
    public Result<Product> getProductDetail(@PathVariable Long id) {
        Product result = productService.getProductById(id);
        return Result.success(result);
    }

    /**
     * 获取笔记关联的商品列表
     * GET /api/v1/shop/notes/{noteId}/products
     */
    @GetMapping("/notes/{noteId}/products")
    public Result<List<Product>> getNoteProducts(@PathVariable Long noteId) {
        List<Product> result = productService.getNoteProducts(noteId);
        return Result.success(result);
    }
}
