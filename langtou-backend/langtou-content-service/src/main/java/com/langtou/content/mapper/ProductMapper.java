package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 按创作者查询商品列表
     */
    @Select("SELECT * FROM products WHERE creator_id = #{creatorId} ORDER BY created_at DESC")
    List<Product> selectByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 按分类查询商品列表
     */
    @Select("SELECT * FROM products WHERE category = #{category} AND status = 'AVAILABLE' ORDER BY created_at DESC")
    List<Product> selectByCategory(@Param("category") String category);

    /**
     * 按创作者和状态查询商品列表
     */
    @Select("SELECT * FROM products WHERE creator_id = #{creatorId} AND status = #{status} ORDER BY created_at DESC")
    List<Product> selectByCreatorIdAndStatus(@Param("creatorId") Long creatorId, @Param("status") String status);

    /**
     * 增加点击量
     */
    @Update("UPDATE products SET click_count = click_count + 1 WHERE id = #{id}")
    int incrementClickCount(@Param("id") Long id);

    /**
     * 增加销量
     */
    @Update("UPDATE products SET sales_count = sales_count + 1 WHERE id = #{id}")
    int incrementSalesCount(@Param("id") Long id);

    /**
     * 统计创作者的商品数量
     */
    @Select("SELECT COUNT(*) FROM products WHERE creator_id = #{creatorId} AND status = 'AVAILABLE'")
    int countByCreatorId(@Param("creatorId") Long creatorId);
}
