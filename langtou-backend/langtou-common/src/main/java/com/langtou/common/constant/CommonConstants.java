package com.langtou.common.constant;

public interface CommonConstants {

    String TOKEN_HEADER = "Authorization";
    String TOKEN_PREFIX = "Bearer ";

    String REQUEST_USER_ID = "userId";
    String REQUEST_USERNAME = "username";

    String DEFAULT_AVATAR = "https://cdn.langtou.com/default-avatar.png";

    Integer STATUS_ENABLE = 1;
    Integer STATUS_DISABLE = 0;

    Integer CONTENT_TYPE_NOTE = 1;
    Integer CONTENT_TYPE_VIDEO = 2;

    Integer GENDER_UNKNOWN = 0;
    Integer GENDER_MALE = 1;
    Integer GENDER_FEMALE = 2;
}
