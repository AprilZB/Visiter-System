package com.maitong.visitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
