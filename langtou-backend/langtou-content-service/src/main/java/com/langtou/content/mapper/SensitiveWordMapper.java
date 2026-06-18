package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.content.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

    /**
     * 查询所有启用的敏感词
     */
    @Select("SELECT word FROM sensitive_word WHERE status = 'ENABLED'")
    List<String> selectAllEnabledWords();

    /**
     * 根据敏感词内容查询
     */
    @Select("SELECT * FROM sensitive_word WHERE word = #{word} LIMIT 1")
    SensitiveWord selectByWord(@Param("word") String word);

    /**
     * 批量查询敏感词（根据ID列表）
     */
    List<SensitiveWord> selectBatchByIds(@Param("ids") List<Long> ids);

    /**
     * 按状态查询敏感词列表
     */
    @Select("SELECT * FROM sensitive_word WHERE status = #{status} ORDER BY create_time DESC")
    List<SensitiveWord> selectByStatus(@Param("status") String status);

    /**
     * 分页查询敏感词列表
     */
    IPage<SensitiveWord> selectPageByCondition(Page<SensitiveWord> page,
                                                @Param("word") String word,
                                                @Param("category") String category,
                                                @Param("source") String source,
                                                @Param("status") String status);

    /**
     * 根据来源查询敏感词列表
     */
    @Select("SELECT * FROM sensitive_word WHERE source = #{source} ORDER BY create_time DESC")
    List<SensitiveWord> selectBySource(@Param("source") String source);
}
