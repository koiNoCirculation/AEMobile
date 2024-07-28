package dev.youtiao.aemobile.web;

import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;

@SpringBootApplication
@Configuration
@ComponentScan
@EnableAutoConfiguration(exclude = {org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration.class})
public class WebApplication extends Thread implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    @Override
    public void run() {
        SpringApplication app = new SpringApplication(WebApplication.class);
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("server.port", "44444");
        hm.put("server.compression.enabled", "true");
        hm.put("server.compression.mime-types", "text/html,text/css,application/javascript,application/json");
        hm.put("spring.mvc.async.request-timeout", "1800000");
        app.setDefaultProperties(hm);
            app.run(WebApplication.class, new String[]{});
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
