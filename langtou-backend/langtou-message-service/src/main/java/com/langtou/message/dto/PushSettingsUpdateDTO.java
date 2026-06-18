package com.langtou.message.dto;

import lombok.Data;

/**
 * 更新推送设置请求DTO
 */
@Data
public class PushSettingsUpdateDTO {

    /** 私信推送开关 */
    private Boolean privateMessageEnabled;

    /** 互动通知推送开关 */
    private Boolean interactionEnabled;

    /** 系统通知推送开关 */
    private Boolean systemEnabled;

    /** 营销推送开关 */
    private Boolean marketingEnabled;

    /** 免打扰开始时间 */
    private String quietHoursStart;

    /** 免打扰结束时间 */
    private String quietHoursEnd;

    /** 每日推送上限 */
    private Integer dailyLimit;
}
