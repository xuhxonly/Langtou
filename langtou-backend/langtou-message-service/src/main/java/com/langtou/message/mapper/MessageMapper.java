package com.langtou.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM message WHERE receiver_id = #{receiverId} AND deleted = 0 ORDER BY create_time DESC")
    List<Message> selectByReceiverId(@Param("receiverId") Long receiverId);

    @Select("SELECT * FROM message WHERE (sender_id = #{userId} AND receiver_id = #{targetId}) OR (sender_id = #{targetId} AND receiver_id = #{userId}) AND deleted = 0 ORDER BY create_time ASC")
    List<Message> selectConversation(@Param("userId") Long userId, @Param("targetId") Long targetId);

    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{receiverId} AND is_read = 0 AND deleted = 0")
    Long countUnread(@Param("receiverId") Long receiverId);

    @Update("UPDATE message SET is_read = 1 WHERE receiver_id = #{receiverId} AND sender_id = #{senderId} AND is_read = 0 AND deleted = 0")
    int markAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    /**
     * 查询与某个用户相关的所有会话的对方用户ID列表（去重）
     */
    @Select("SELECT DISTINCT CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS target_id " +
            "FROM message WHERE (sender_id = #{userId} OR receiver_id = #{userId}) AND deleted = 0")
    List<Long> selectConversationTargetIds(@Param("userId") Long userId);

    /**
     * 查询两个用户之间的最新一条消息
     */
    @Select("SELECT * FROM message WHERE (sender_id = #{userId} AND receiver_id = #{targetId}) " +
            "OR (sender_id = #{targetId} AND receiver_id = #{userId}) " +
            "AND deleted = 0 ORDER BY create_time DESC LIMIT 1")
    Message selectLatestMessage(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 查询某个用户从某个目标用户收到的未读消息数
     */
    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{receiverId} AND sender_id = #{senderId} AND is_read = 0 AND deleted = 0")
    Long countUnreadFromSender(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);
}
