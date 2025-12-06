package dev.youtiao.aemobile.web.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class DefaultController {
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
