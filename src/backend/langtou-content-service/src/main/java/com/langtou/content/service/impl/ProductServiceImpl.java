package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.content.entity.NoteProduct;
import com.langtou.content.entity.Product;
import com.langtou.content.mapper.NoteProductMapper;
import com.langtou.content.mapper.ProductMapper;
import com.langtou.content.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final NoteProductMapper noteProductMapper;

    /**
     * 一个笔记最多关联的商品数量
     */
    private static final int MAX_NOTE_PRODUCTS = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product createProduct(Product product, Long creatorId) {
        // 参数校验
        if (!StringUtils.hasText(product.getName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (product.getPrice() == null || product.getPrice().doubleValue() <= 0) {
            throw new BusinessException("商品价格必须大于0");
        }
        if (!StringUtils.hasText(product.getImageUrl())) {
            throw new BusinessException("商品主图不能为空");
        }

        product.setCreatorId(creatorId);
        product.setStatus("AVAILABLE");
        product.setSalesCount(0);
        product.setClickCount(0);

        productMapper.insert(product);
        log.info("商品创建成功: id={}, creatorId={}, name={}", product.getId(), creatorId, product.getName());
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product updateProduct(Long productId, Long creatorId, Product product) {
        Product existing = productMapper.selectById(productId);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        if (!existing.getCreatorId().equals(creatorId)) {
            throw new BusinessException("无权编辑该商品");
        }

        // 更新非空字段
        if (StringUtils.hasText(product.getName())) {
            existing.setName(product.getName());
        }
        if (product.getDescription() != null) {
            existing.setDescription(product.getDescription());
        }
        if (product.getPrice() != null && product.getPrice().doubleValue() > 0) {
            existing.setPrice(product.getPrice());
        }
        if (product.getOriginalPrice() != null) {
            existing.setOriginalPrice(product.getOriginalPrice());
        }
        if (StringUtils.hasText(product.getImageUrl())) {
            existing.setImageUrl(product.getImageUrl());
        }
        if (product.getImages() != null) {
            existing.setImages(product.getImages());
        }
        if (StringUtils.hasText(product.getCategory())) {
            existing.setCategory(product.getCategory());
        }
        if (StringUtils.hasText(product.getExternalUrl())) {
            existing.setExternalUrl(product.getExternalUrl());
        }
        if (product.getCommissionRate() != null) {
            existing.setCommissionRate(product.getCommissionRate());
        }

        productMapper.updateById(existing);
        log.info("商品更新成功: id={}, creatorId={}", productId, creatorId);
        return existing;
    }

    @Override
    public Product getProductById(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }

    @Override
    public PageResult<Product> getMyProducts(Long creatorId, String status, int page, int size) {
        Page<Product> pageParam = new Page<>(page, size);
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("creator_id", creatorId);
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_at");
        Page<Product> result = productMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResult<Product> getProductsByCategory(String category, int page, int size) {
        Page<Product> pageParam = new Page<>(page, size);
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("category", category)
                .eq("status", "AVAILABLE")
                .orderByDesc("created_at");
        Page<Product> result = productMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product toggleProductStatus(Long productId, Long creatorId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getCreatorId().equals(creatorId)) {
            throw new BusinessException("无权操作该商品");
        }

        if ("AVAILABLE".equals(product.getStatus())) {
            product.setStatus("UNAVAILABLE");
        } else {
            product.setStatus("AVAILABLE");
        }
        productMapper.updateById(product);
        log.info("商品状态切换: id={}, newStatus={}", productId, product.getStatus());
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkProductsToNote(Long noteId, Long creatorId, List<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            throw new BusinessException("关联商品不能为空");
        }
        if (productIds.size() > MAX_NOTE_PRODUCTS) {
            throw new BusinessException("一个笔记最多关联" + MAX_NOTE_PRODUCTS + "个商品");
        }

        // 检查当前笔记已关联的商品数量
        int existingCount = noteProductMapper.countByNoteId(noteId);
        if (existingCount + productIds.size() > MAX_NOTE_PRODUCTS) {
            throw new BusinessException("一个笔记最多关联" + MAX_NOTE_PRODUCTS + "个商品");
        }

        // 查询已有正常关联的商品ID
        List<Long> existingProductIds = noteProductMapper.selectProductIdsByNoteId(noteId);

        int sortOrder = existingCount;
        for (Long productId : productIds) {
            // 去重检查
            if (existingProductIds.contains(productId)) {
                continue;
            }

            // 验证商品存在且属于该创作者
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException("商品不存在: " + productId);
            }
            if (!product.getCreatorId().equals(creatorId)) {
                throw new BusinessException("无权关联该商品: " + productId);
            }

            NoteProduct noteProduct = new NoteProduct();
            noteProduct.setNoteId(noteId);
            noteProduct.setProductId(productId);
            noteProduct.setCreatorId(creatorId);
            noteProduct.setSortOrder(sortOrder++);
            noteProduct.setStatus(1);
            noteProduct.setClickCount(0);
            noteProductMapper.insert(noteProduct);
        }

        log.info("笔记关联商品成功: noteId={}, creatorId={}, productCount={}", noteId, creatorId, productIds.size());
    }

    @Override
    public List<Product> getNoteProducts(Long noteId) {
        List<NoteProduct> noteProducts = noteProductMapper.selectByNoteId(noteId);
        if (CollectionUtils.isEmpty(noteProducts)) {
            return new ArrayList<>();
        }

        List<Long> productIds = noteProducts.stream()
                .map(NoteProduct::getProductId)
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);

        // 按sortOrder排序
        return products.stream()
                .sorted((a, b) -> {
                    int orderA = 0, orderB = 0;
                    for (NoteProduct np : noteProducts) {
                        if (np.getProductId().equals(a.getId())) orderA = np.getSortOrder();
                        if (np.getProductId().equals(b.getId())) orderB = np.getSortOrder();
                    }
                    return Integer.compare(orderA, orderB);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordProductClick(Long productId, Long noteId) {
        productMapper.incrementClickCount(productId);
        if (noteId != null) {
            noteProductMapper.incrementClickCount(noteId, productId);
        }
        log.info("记录商品点击: productId={}, noteId={}", productId, noteId);
    }

    @Override
    public PageResult<Product> getCreatorShowcase(Long creatorId, int page, int size) {
        Page<Product> pageParam = new Page<>(page, size);
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("creator_id", creatorId)
                .eq("status", "AVAILABLE")
                .orderByDesc("created_at");
        Page<Product> result = productMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }
}
