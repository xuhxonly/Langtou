package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.ActivityParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityParticipantMapper extends BaseMapper<ActivityParticipant> {

    /**
     * 检查用户是否已参与活动
     */
    @Select("SELECT COUNT(*) FROM activity_participants WHERE activity_id = #{activityId} AND user_id = #{userId}")
    int existsByActivityAndUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /**
     * 统计活动参与人数
     */
    @Select("SELECT COUNT(*) FROM activity_participants WHERE activity_id = #{activityId}")
    int countByActivityId(@Param("activityId") Long activityId);

    /**
     * 增加用户参与笔记数
     */
    @Update("UPDATE activity_participants SET note_count = note_count + 1 WHERE activity_id = #{activityId} AND user_id = #{userId}")
    int incrementNoteCount(@Param("activityId") Long activityId, @Param("userId") Long userId);
}
