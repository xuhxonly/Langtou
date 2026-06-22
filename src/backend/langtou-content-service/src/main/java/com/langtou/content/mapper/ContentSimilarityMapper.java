package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.ContentSimilarity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ContentSimilarityMapper extends BaseMapper<ContentSimilarity> {

    /**
     * 查询与指定内容高度相似的内容列表
     */
    @Select("SELECT * FROM content_similarities " +
            "WHERE (content_id_a = #{contentId} OR content_id_b = #{contentId}) " +
            "AND similarity_score >= #{threshold} " +
            "ORDER BY similarity_score DESC")
    List<ContentSimilarity> findSimilarContent(@Param("contentId") Long contentId,
                                                @Param("threshold") BigDecimal threshold);
}
