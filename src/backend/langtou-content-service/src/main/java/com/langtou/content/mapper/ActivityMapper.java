package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 增加参与人数
     */
    @Update("UPDATE activities SET participant_count = participant_count + 1 WHERE id = #{activityId}")
    int incrementParticipantCount(@Param("activityId") Long activityId);

    /**
     * 减少参与人数
     */
    @Update("UPDATE activities SET participant_count = participant_count - 1 WHERE id = #{activityId} AND participant_count > 0")
    int decrementParticipantCount(@Param("activityId") Long activityId);

    /**
     * 增加笔记数
     */
    @Update("UPDATE activities SET note_count = note_count + 1 WHERE id = #{activityId}")
    int incrementNoteCount(@Param("activityId") Long activityId);

    /**
     * 增加浏览量
     */
    @Update("UPDATE activities SET total_views = total_views + 1 WHERE id = #{activityId}")
    int incrementViewCount(@Param("activityId") Long activityId);

    /**
     * 增加互动量
     */
    @Update("UPDATE activities SET total_interactions = total_interactions + #{delta} WHERE id = #{activityId}")
    int incrementInteractionCount(@Param("activityId") Long activityId, @Param("delta") int delta);
}
