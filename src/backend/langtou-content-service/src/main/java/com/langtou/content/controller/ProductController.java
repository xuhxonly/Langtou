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
import java.util.Map;

/**
 * 商品管理 - 创作者端
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "商品服务", description = "商品相关接口")
    public class ProductController {

    private final ProductService productService;

    /**
     * 创建商品
     * POST /api/v1/products
     */
    @PostMapping
    public Result<Product> createProduct(@RequestBody Product product,
                                          @RequestHeader("X-User-Id") Long userId) {
        Product result = productService.createProduct(product, userId);
        return Result.success("商品创建成功", result);
    }

    /**
     * 更新商品
     * PUT /api/v1/products/{id}
     */
    @PutMapping("/{id}")
    public Result<Product> updateProduct(@PathVariable Long id,
                                          @RequestBody Product product,
                                          @RequestHeader("X-User-Id") Long userId) {
        Product result = productService.updateProduct(id, userId, product);
        return Result.success("商品更新成功", result);
    }

    /**
     * 我的商品列表
     * GET /api/v1/products
     */
    @GetMapping
    public Result<PageResult<Product>> getMyProducts(@RequestHeader("X-User-Id") Long userId,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        PageResult<Product> result = productService.getMyProducts(userId, status, page, size);
        return Result.success(result);
    }

    /**
     * 商品详情
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product result = productService.getProductById(id);
        return Result.success(result);
    }

    /**
     * 上架/下架商品
     * POST /api/v1/products/{id}/toggle-status
     */
    @PostMapping("/{id}/toggle-status")
    public Result<Product> toggleProductStatus(@PathVariable Long id,
                                                @RequestHeader("X-User-Id") Long userId) {
        Product result = productService.toggleProductStatus(id, userId);
        return Result.success("状态切换成功", result);
    }

    /**
     * 笔记关联商品
     * POST /api/v1/notes/{noteId}/products
     */
    @PostMapping("/notes/{noteId}/products")
    public Result<Void> linkProductsToNote(@PathVariable Long noteId,
                                            @RequestHeader("X-User-Id") Long userId,
                                            @RequestBody Map<String, List<Long>> body) {
        List<Long> productIds = body.get("productIds");
        productService.linkProductsToNote(noteId, userId, productIds);
        return Result.success("关联商品成功");
    }

    /**
     * 记录商品点击
     * POST /api/v1/products/{id}/click
     */
    @PostMapping("/{id}/click")
    public Result<Void> recordProductClick(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, Long> body) {
        Long noteId = body != null ? body.get("noteId") : null;
        productService.recordProductClick(id, noteId);
        return Result.success();
    }
}
