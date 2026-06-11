package com.langtou.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "系统繁忙，请稍后重试"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方式不支持"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_ALREADY_EXISTS(1003, "用户已存在"),
    USER_NOT_LOGIN(1004, "用户未登录"),
    TOKEN_INVALID(1005, "Token无效或已过期"),
    TOKEN_EXPIRED(1006, "Token已过期"),

    CONTENT_NOT_FOUND(2001, "内容不存在"),
    CONTENT_PUBLISH_FAILED(2002, "内容发布失败"),

    INTERACT_FAILED(3001, "操作失败"),
    ALREADY_LIKED(3002, "已经点赞过了"),
    NOT_LIKED(3003, "还未点赞"),
    ALREADY_COLLECTED(3004, "已经收藏过了"),
    NOT_COLLECTED(3005, "还未收藏"),
    CANNOT_COLLECT_OWN(3006, "不能收藏自己的笔记"),

    MESSAGE_SEND_FAILED(4001, "消息发送失败"),
    NOTIFICATION_NOT_FOUND(5001, "通知不存在"),

    FOLLOW_ALREADY(5002, "已经关注过了"),
    FOLLOW_NOT_YET(5003, "还未关注"),
    FOLLOW_SELF(5004, "不能关注自己"),
    FOLLOW_CANCEL_FAILED(5005, "取消关注失败"),

    FILE_UPLOAD_FAILED(6001, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORTED(6002, "不支持的文件类型");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
