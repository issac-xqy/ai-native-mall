package org.example.java_ai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiNativeMallOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Native Smart Mall API")
                        .version("1.0.0")
                        .description("AI原生智能商城 — Spring Boot 3.2 + JDK 21 + LangChain4j")
                        .contact(new Contact().name("xqy").email("admin@example.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录获取 token，格式: Bearer <token>")));
    }
}
