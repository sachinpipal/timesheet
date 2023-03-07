package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Spring Security configuration file for handling all cross-cutting security task
 */
@Configuration
public class SwaggerConfig {


    /**
     * Swagger configuration for API scanning and api related information.
     *
     * @return the docket object for Swagger
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("APIs Documentation")
                .description("The Timesheet Management API documentation ").termsOfServiceUrl("All rights reserved to Nagarro")
                .version("1.0.0")
                .build();
    }

}