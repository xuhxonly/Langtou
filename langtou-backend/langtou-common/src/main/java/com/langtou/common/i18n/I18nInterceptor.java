package com.langtou.common.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 国际化拦截器
 *
 * 从请求头 Accept-Language 获取语言设置，存入ThreadLocal，
 * 请求结束后清理ThreadLocal，防止内存泄漏。
 *
 * 支持的语言格式：
 * - Accept-Language: zh_CN
 * - Accept-Language: en-US
 * - Accept-Language: zh
 *
 * 默认语言：中文（zh_CN）
 */
@Slf4j
public class I18nInterceptor implements HandlerInterceptor {

    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String language = request.getHeader(ACCEPT_LANGUAGE_HEADER);
        if (language != null && !language.isEmpty()) {
            // 处理可能包含权重值的Accept-Language头，如 "zh-CN,zh;q=0.9,en;q=0.8"
            String primaryLanguage = parseAcceptLanguage(language);
            I18nService.setLocale(primaryLanguage);
            log.debug("设置语言上下文: {}", primaryLanguage);
        } else {
            // 未设置Accept-Language时，使用默认中文
            I18nService.setLocale("zh_CN");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清理ThreadLocal，防止内存泄漏
        I18nService.clearLocale();
    }

    /**
     * 解析Accept-Language头，提取首选语言
     *
     * Accept-Language格式示例：
     * - "zh-CN,zh;q=0.9,en;q=0.8"
     * - "en-US"
     * - "zh_CN"
     *
     * @param acceptLanguage Accept-Language头值
     * @return 首选语言代码
     */
    private String parseAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return "zh_CN";
        }

        // 取第一个语言（逗号分隔，可能带权重）
        String firstLang = acceptLanguage.split(",")[0].trim();

        // 去除权重参数（如 "zh;q=0.9"）
        int semicolonIndex = firstLang.indexOf(';');
        if (semicolonIndex > 0) {
            firstLang = firstLang.substring(0, semicolonIndex).trim();
        }

        return firstLang;
    }
}
