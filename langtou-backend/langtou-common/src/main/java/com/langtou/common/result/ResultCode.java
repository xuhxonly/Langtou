package com.langtou.common.result;

import com.langtou.common.i18n.I18nService;
import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功", "SUCCESS"),
    ERROR(500, "系统繁忙，请稍后重试", "INTERNAL_ERROR"),

    PARAM_ERROR(400, "参数错误", "BAD_REQUEST"),
    UNAUTHORIZED(401, "未授权，请先登录", "UNAUTHORIZED"),
    FORBIDDEN(403, "禁止访问", "FORBIDDEN"),
    NOT_FOUND(404, "资源不存在", "NOT_FOUND"),
    METHOD_NOT_ALLOWED(405, "请求方式不支持", "BAD_REQUEST"),

    USER_NOT_FOUND(1001, "用户不存在", "USER_NOT_FOUND"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误", "FAILED"),
    USER_ALREADY_EXISTS(1003, "用户已存在", "FAILED"),
    USER_NOT_LOGIN(1004, "用户未登录", "UNAUTHORIZED"),
    TOKEN_INVALID(1005, "Token无效或已过期", "UNAUTHORIZED"),
    TOKEN_EXPIRED(1006, "Token已过期", "UNAUTHORIZED"),

    CONTENT_NOT_FOUND(2001, "内容不存在", "CONTENT_NOT_FOUND"),
    CONTENT_PUBLISH_FAILED(2002, "内容发布失败", "FAILED"),

    INTERACT_FAILED(3001, "操作失败", "FAILED"),
    ALREADY_LIKED(3002, "已经点赞过了", "FAILED"),
    NOT_LIKED(3003, "还未点赞", "FAILED"),
    ALREADY_COLLECTED(3004, "已经收藏过了", "FAILED"),
    NOT_COLLECTED(3005, "还未收藏", "FAILED"),
    CANNOT_COLLECT_OWN(3006, "不能收藏自己的笔记", "BAD_REQUEST"),

    MESSAGE_SEND_FAILED(4001, "消息发送失败", "FAILED"),
    NOTIFICATION_NOT_FOUND(5001, "通知不存在", "NOT_FOUND"),

    FOLLOW_ALREADY(5002, "已经关注过了", "FAILED"),
    FOLLOW_NOT_YET(5003, "还未关注", "FAILED"),
    FOLLOW_SELF(5004, "不能关注自己", "BAD_REQUEST"),
    FOLLOW_CANCEL_FAILED(5005, "取消关注失败", "FAILED"),

    FILE_UPLOAD_FAILED(6001, "文件上传失败", "FAILED"),
    FILE_TYPE_NOT_SUPPORTED(6002, "不支持的文件类型", "BAD_REQUEST"),

    DRAFT_NOT_FOUND(7001, "草稿不存在", "NOT_FOUND"),
    DRAFT_SAVE_FAILED(7002, "草稿保存失败", "FAILED"),
    VISIBILITY_INVALID(7003, "无效的可见性设置", "BAD_REQUEST"),
    PIN_FAILED(7004, "笔记置顶失败", "FAILED"),
    USER_BLOCKED(7005, "用户已被屏蔽", "FORBIDDEN"),
    REPORT_NOT_FOUND(7006, "举报记录不存在", "NOT_FOUND"),
    REPORT_HANDLED(7007, "举报已处理", "FAILED"),
    SEARCH_SUGGEST_FAILED(7008, "搜索建议获取失败", "FAILED"),

    TOO_MANY_REQUESTS(8001, "操作过于频繁，请稍后再试", "TOO_MANY_REQUESTS"),

    DEVICE_BLOCKED(9001, "设备已被封禁", "DEVICE_BLOCKED"),
    BEHAVIOR_ANOMALY(9002, "行为异常，请稍后再试", "BEHAVIOR_ANOMALY"),
    DUPLICATE_CONTENT_DETECTED(9003, "内容重复检测失败", "FAILED"),
    FRAUD_REPORT_FAILED(9004, "举报提交失败", "FAILED");

    private final Integer code;
    private final String message;
    private final String messageKey;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.messageKey = null;
    }

    ResultCode(Integer code, String message, String messageKey) {
        this.code = code;
        this.message = message;
        this.messageKey = messageKey;
    }

    /**
     * 获取国际化消息。
     * 优先通过messageKey从资源文件中获取对应语言的消息文本。
     * 如果messageKey为空或未找到国际化消息，则通过错误码获取。
     * 如果仍未找到，返回默认中文消息。
     *
     * @param i18nService 国际化服务实例
     * @return 国际化后的消息文本
     */
    public String getI18nMessage(I18nService i18nService) {
        if (i18nService == null) {
            return this.message;
        }

        // 优先使用messageKey获取国际化消息
        if (this.messageKey != null && !this.messageKey.isEmpty()) {
            String i18nMsg = i18nService.getMessageByKey(this.messageKey);
            if (i18nMsg != null && !i18nMsg.isEmpty() && !i18nMsg.equals("error.default")) {
                return i18nMsg;
            }
        }

        // 回退到错误码方式获取
        String i18nMsg = i18nService.getErrorMessage(this.code);
        if (i18nMsg == null || i18nMsg.isEmpty()) {
            return this.message;
        }
        return i18nMsg;
    }
}
