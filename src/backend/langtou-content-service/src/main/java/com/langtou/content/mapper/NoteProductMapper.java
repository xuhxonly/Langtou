package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.NoteProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NoteProductMapper extends BaseMapper<NoteProduct> {

    /**
     * 查询笔记关联的商品列表（正常状态）
     */
    @Select("SELECT * FROM note_products WHERE note_id = #{noteId} AND status = 1 ORDER BY sort_order ASC")
    List<NoteProduct> selectByNoteId(@Param("noteId") Long noteId);

    /**
     * 查询笔记关联的商品ID列表
     */
    @Select("SELECT product_id FROM note_products WHERE note_id = #{noteId} AND status = 1 ORDER BY sort_order ASC")
    List<Long> selectProductIdsByNoteId(@Param("noteId") Long noteId);

    /**
     * 统计笔记关联的商品数量
     */
    @Select("SELECT COUNT(*) FROM note_products WHERE note_id = #{noteId} AND status = 1")
    int countByNoteId(@Param("noteId") Long noteId);

    /**
     * 增加笔记中商品的点击量
     */
    @Update("UPDATE note_products SET click_count = click_count + 1 WHERE note_id = #{noteId} AND product_id = #{productId}")
    int incrementClickCount(@Param("noteId") Long noteId, @Param("productId") Long productId);
}
