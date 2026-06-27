package com.iqspark.underwriter.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata for the auto-generated Swagger UI (/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI underwriterOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("AI Underwriter Agent")
                .version("1.0")
                .description("AI-first, multi-line P&C underwriting decision-support API "
                        + "(vacant home is the first line built and worked reference example)."));
    }
}
