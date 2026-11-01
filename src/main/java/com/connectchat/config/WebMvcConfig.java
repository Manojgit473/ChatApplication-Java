package com.connectchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring Web MVC Configuration for static resources and upload directories.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${connectchat.upload.dir:uploads/attachments}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static CSS, JS, and Icons
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Local file storage resource mapping
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}
