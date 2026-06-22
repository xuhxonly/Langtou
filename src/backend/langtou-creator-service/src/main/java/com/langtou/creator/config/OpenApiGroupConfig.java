package com.langtou.creator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    @Bean
    public GroupedOpenApi creatorGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("creator")
                .displayName("creator API")
                .pathsToMatch("/api/v1/(creator|wallet|commission|revenue|analytics).*")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info()
                                .title("CREATOR Service API")
                                .version("1.0.0")
                                .contact(new Contact().name("Langtou Team").email("support@langtou.com"))
                                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                        .components(new Components()
                                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .name(SECURITY_SCHEME_NAME)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("请输入 JWT Token"))));
    }
}