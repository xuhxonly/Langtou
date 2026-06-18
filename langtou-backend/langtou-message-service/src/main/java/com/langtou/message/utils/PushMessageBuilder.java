package com.langtou.message.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 推送消息构建工具类
 * 用于根据推送场景模板构建推送标题、内容和附加数据
 */
public class PushMessageBuilder {

    /** 推送场景名称前缀 */
    private static final String TITLE_PREFIX = "榔头 - ";

    // ===== 模板常量 =====
    public static final String TEMPLATE_PRIVATE_MSG = "PUSH_PRIVATE_MSG";
    public static final String TEMPLATE_LIKE = "PUSH_LIKE";
    public static final String TEMPLATE_COMMENT = "PUSH_COMMENT";
    public static final String TEMPLATE_COLLECT = "PUSH_COLLECT";
    public static final String TEMPLATE_FOLLOW = "PUSH_FOLLOW";
    public static final String TEMPLATE_REPLY = "PUSH_REPLY";
    public static final String TEMPLATE_MENTION = "PUSH_MENTION";
    public static final String TEMPLATE_REVIEW_PASS = "PUSH_REVIEW_PASS";
    public static final String TEMPLATE_REVIEW_REJECT = "PUSH_REVIEW_REJECT";
    public static final String TEMPLATE_ACTIVITY = "PUSH_ACTIVITY";
    public static final String TEMPLATE_MARKETING = "PUSH_MARKETING";

    private PushMessageBuilder() {
        // 工具类禁止实例化
    }

    // ===== 场景名称映射 =====
    private static final Map<String, String> SCENE_NAME_MAP = new HashMap<>();
    static {
        SCENE_NAME_MAP.put("PRIVATE_MESSAGE", "新私信");
        SCENE_NAME_MAP.put("INTERACTION", "互动通知");
        SCENE_NAME_MAP.put("SYSTEM", "系统通知");
        SCENE_NAME_MAP.put("MARKETING", "精选推荐");
    }

    /**
     * 构建私信推送消息
     *
     * @param senderName 发送者昵称
     * @param msgPreview 消息预览（截取前30字）
     * @param data       附加数据（conversationId, senderId等）
     * @return 构建结果 [title, body, data]
     */
    public static Map<String, String> buildPrivateMessage(String senderName, String msgPreview, Map<String, String> data) {
        String body = senderName + ": " + truncate(msgPreview, 30);
        return buildMessage("PRIVATE_MESSAGE", body, data);
    }

    /**
     * 构建互动通知推送消息（点赞）
     *
     * @param actorName  操作者昵称
     * @param noteTitle  笔记标题
     * @param data       附加数据
     * @return 构建结果
     */
    public static Map<String, String> buildLikeNotification(String actorName, String noteTitle, Map<String, String> data) {
        String body = actorName + " 赞了你的笔记《" + truncate(noteTitle, 30) + "》";
        return buildMessage("INTERACTION", body, data);
    }

    /**
     * 构建互动通知推送消息（评论）
     *
     * @param actorName       操作者昵称
     * @param commentPreview  评论预览
     * @param data            附加数据
     * @return 构建结果
     */
    public static Map<String, String> buildCommentNotification(String actorName, String commentPreview, Map<String, String> data) {
        String body = actorName + " 评论了你的笔记: " + truncate(commentPreview, 30);
        return buildMessage("INTERACTION", body, data);
    }

    /**
     * 构建互动通知推送消息（收藏）
     *
     * @param actorName 操作者昵称
     * @param noteTitle 笔记标题
     * @param data      附加数据
     * @return 构建结果
     */
    public static Map<String, String> buildCollectNotification(String actorName, String noteTitle, Map<String, String> data) {
        String body = actorName + " 收藏了你的笔记《" + truncate(noteTitle, 30) + "》";
        return buildMessage("INTERACTION", body, data);
    }

    /**
     * 构建互动通知推送消息（关注）
     *
     * @param actorName 操作者昵称
     * @param data      附加数据
     * @return 构建结果
     */
    public static Map<String, String> buildFollowNotification(String actorName, Map<String, String> data) {
        String body = actorName + " 关注了你";
        return buildMessage("INTERACTION", body, data);
    }

    /**
     * 构建系统通知推送消息（审核通过）
     *
     * @param noteTitle 笔记标题
     * @param data      附加数据
     * @return 构建结果
     */
    public static Map<String, String> buildReviewPassNotification(String noteTitle, Map<String, String> data) {
        String body = "你的笔记《" + truncate(noteTitle, 30) + "》已通过审核，快去看看吧！";
        return buildMessage("SYSTEM", body, data);
    }

    /**
     * 构建系统通知推送消息（审核驳回）
     *
     * @param noteTitle 笔记标题
     * @param reason    驳回原因
     * @param data      附加数据
     * @return 构建结果
     */
    public static Map<String, String> buildReviewRejectNotification(String noteTitle, String reason, Map<String, String> data) {
        String body = "你的笔记《" + truncate(noteTitle, 30) + "》未通过审核，原因: " + truncate(reason, 50);
        return buildMessage("SYSTEM", body, data);
    }

    /**
     * 构建营销推送消息
     *
     * @param content 营销内容
     * @param data    附加数据
     * @return 构建结果
     */
    public static Map<String, String> buildMarketingPush(String content, Map<String, String> data) {
        return buildMessage("MARKETING", content, data);
    }

    /**
     * 通用消息构建方法
     *
     * @param pushType 推送类型
     * @param body     推送内容
     * @param data     附加数据
     * @return 包含 title, body, data 的Map
     */
    public static Map<String, String> buildMessage(String pushType, String body, Map<String, String> data) {
        Map<String, String> message = new HashMap<>();
        String sceneName = SCENE_NAME_MAP.getOrDefault(pushType, "通知");
        message.put("title", TITLE_PREFIX + sceneName);
        message.put("body", body);
        if (data != null) {
            message.putAll(data);
        }
        return message;
    }

    /**
     * 截取字符串到指定最大长度
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
