package dev.youtiao.aemobile.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class StaticFileConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String workingdirectory = System.getProperty("user.dir");
        String iconsPath = "file:" + workingdirectory + File.separator + "dumps/itempanel_icons/";
        iconsPath.replace('\\','/');
        registry.addResourceHandler("/icons/**")
                .addResourceLocations(iconsPath);
    }
}
