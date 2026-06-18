package com.langtou.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

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

    /**
     * 批量查询多个会话的最新消息（避免N+1查询）
     * 使用 UNION ALL 合并多个子查询，每个子查询取两个用户间最新一条消息
     */
    @Select("<script>" +
            "SELECT m.* FROM message m INNER JOIN (" +
            "<foreach collection='targetIds' item='targetId' separator='UNION ALL'>" +
            "SELECT MAX(id) AS max_id FROM message WHERE " +
            "((sender_id = #{userId} AND receiver_id = #{targetId}) OR " +
            "(sender_id = #{targetId} AND receiver_id = #{userId})) AND deleted = 0" +
            "</foreach>" +
            ") latest ON m.id = latest.max_id" +
            "</script>")
    List<Message> batchSelectLatestMessages(@Param("userId") Long userId, @Param("targetIds") List<Long> targetIds);

    /**
     * 批量查询多个目标用户的未读消息数（避免N+1查询）
     */
    @Select("<script>" +
            "SELECT sender_id AS targetId, COUNT(*) AS unreadCount FROM message " +
            "WHERE receiver_id = #{receiverId} AND is_read = 0 AND deleted = 0 " +
            "AND sender_id IN " +
            "<foreach collection='targetIds' item='targetId' open='(' separator=',' close=')'>" +
            "#{targetId}" +
            "</foreach>" +
            " GROUP BY sender_id" +
            "</script>")
    List<Map<String, Object>> batchCountUnreadFromSenders(@Param("receiverId") Long receiverId, @Param("targetIds") List<Long> targetIds);
}
