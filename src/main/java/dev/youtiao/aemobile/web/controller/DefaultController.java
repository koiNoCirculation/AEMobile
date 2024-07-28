package dev.youtiao.aemobile.web.controller;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class DefaultController {
    @GetMapping("/AEMobileTest")
    public String defaultFunc() {
        return "<h1>hello world</h1>";
    }
}
