package com.langtou.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化服务
 *
 * 基于ThreadLocal的语言上下文，支持中文/英文切换。
 * 通过Spring MessageSource加载i18n资源文件中的消息。
 *
 * 使用方式：
 * 1. 通过请求头 Accept-Language 设置语言（I18nInterceptor自动处理）
 * 2. 通过 I18nService.setLocale() 手动设置语言上下文
 * 3. 通过 I18nService.getMessage() 获取国际化消息
 */
@Component
public class I18nService {

    private static final ThreadLocal<Locale> localeHolder = new ThreadLocal<>();

    private final MessageSource messageSource;

    public I18nService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 设置当前线程的语言上下文
     *
     * @param locale 语言Locale对象
     */
    public static void setLocale(Locale locale) {
        if (locale != null) {
            localeHolder.set(locale);
        }
    }

    /**
     * 设置当前线程的语言上下文（通过语言代码）
     *
     * @param language 语言代码，如 "zh_CN", "en_US"
     */
    public static void setLocale(String language) {
        if (language != null && !language.isEmpty()) {
            Locale locale = parseLocale(language);
            localeHolder.set(locale);
        }
    }

    /**
     * 获取当前线程的语言上下文
     *
     * @return 当前Locale，默认中文
     */
    public static Locale getLocale() {
        Locale locale = localeHolder.get();
        if (locale == null) {
            // 尝试从Spring LocaleContextHolder获取
            locale = LocaleContextHolder.getLocale();
        }
        if (locale == null || locale.getLanguage().isEmpty()) {
            locale = Locale.SIMPLIFIED_CHINESE;
        }
        return locale;
    }

    /**
     * 清除当前线程的语言上下文
     */
    public static void clearLocale() {
        localeHolder.remove();
    }

    /**
     * 获取国际化消息（使用当前线程语言上下文）
     *
     * @param code 消息代码（对应properties文件中的key）
     * @return 国际化后的消息文本
     */
    public String getMessage(String code) {
        return getMessage(code, null, getLocale());
    }

    /**
     * 获取国际化消息（带参数，使用当前线程语言上下文）
     *
     * @param code 消息代码
     * @param args 消息参数（用于占位符替换）
     * @return 国际化后的消息文本
     */
    public String getMessage(String code, Object... args) {
        return getMessage(code, args, getLocale());
    }

    /**
     * 根据语言获取国际化消息
     *
     * @param code 消息代码（对应properties文件中的key）
     * @param lang 语言代码，如 "zh_CN", "en_US"，为空时默认中文
     * @return 国际化后的消息文本
     */
    public String getMessage(String code, String lang) {
        Locale locale = parseLocale(lang);
        return getMessage(code, null, locale);
    }

    /**
     * 根据语言获取国际化消息（带参数）
     *
     * @param code 消息代码
     * @param lang 语言代码，如 "zh_CN", "en_US"，为空时默认中文
     * @param args 消息参数（用于占位符替换）
     * @return 国际化后的消息文本
     */
    public String getMessage(String code, String lang, Object... args) {
        Locale locale = parseLocale(lang);
        return getMessage(code, args, locale);
    }

    /**
     * 获取国际化消息（指定Locale）
     *
     * @param code   消息代码
     * @param args   消息参数
     * @param locale 指定语言
     * @return 国际化后的消息文本
     */
    public String getMessage(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            // 找不到消息时返回code本身
            return code;
        }
    }

    /**
     * 获取错误消息的国际化文本
     * 用于ResultCode的国际化消息获取
     *
     * @param resultCode 错误码
     * @return 国际化后的错误消息
     */
    public String getErrorMessage(int resultCode) {
        String code = "error." + resultCode;
        String message = getMessage(code);
        // 如果没有找到对应的国际化消息，返回默认消息
        if (code.equals(message)) {
            return getMessage("error.default");
        }
        return message;
    }

    /**
     * 根据messageKey获取国际化消息
     * 用于ResultCode通过messageKey字段获取语义化的国际化消息
     *
     * @param messageKey 消息key，如 "SUCCESS", "USER_NOT_FOUND"
     * @return 国际化后的消息文本
     */
    public String getMessageByKey(String messageKey) {
        if (messageKey == null || messageKey.isEmpty()) {
            return getMessage("error.default");
        }
        String message = getMessage(messageKey);
        // 如果没有找到对应的消息，返回key本身
        if (messageKey.equals(message)) {
            return getMessage("error.default");
        }
        return message;
    }

    /**
     * 解析语言代码字符串为Locale对象
     *
     * @param language 语言代码，如 "zh_CN", "en-US", "zh"
     * @return Locale对象
     */
    private static Locale parseLocale(String language) {
        if (language == null || language.isEmpty()) {
            return Locale.SIMPLIFIED_CHINESE;
        }

        // 标准化分隔符
        language = language.replace("-", "_");

        String[] parts = language.split("_");
        if (parts.length >= 2) {
            return new Locale(parts[0], parts[1]);
        } else if (parts.length == 1) {
            switch (parts[0].toLowerCase()) {
                case "zh":
                    return Locale.SIMPLIFIED_CHINESE;
                case "en":
                    return Locale.US;
                default:
                    return new Locale(parts[0]);
            }
        }

        return Locale.SIMPLIFIED_CHINESE;
    }
}
