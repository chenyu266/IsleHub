package com.islehub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI isleHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IsleHub API")
                        .version("1.0.0")
                        .description("IsleHub 商城系统接口文档"));
    }
}
