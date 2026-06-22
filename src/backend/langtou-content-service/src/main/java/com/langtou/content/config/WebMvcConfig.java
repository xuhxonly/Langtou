package com.langtou.content.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${langtou.upload.path:/data/uploads/langtou/}")
    private String uploadPath;

    @Value("${langtou.upload.url-prefix:/uploads/}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations("file:" + uploadPath);
    }
}
