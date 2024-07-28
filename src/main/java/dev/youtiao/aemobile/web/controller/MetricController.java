package dev.youtiao.aemobile.web.controller;

import dev.youtiao.aemobile.web.service.MSPTRealImpl;
import dev.youtiao.aemobile.web.service.MSPTService;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Arrays;

@RestController
public class MetricController {
    @Resource(type = MSPTRealImpl.class)
    private MSPTService msptService;

    @GetMapping("/api/serverMSPT")
    public Flux<ServerSentEvent<String>> mspt() {
        //5s, 30s, 300s
        return Flux.interval(Duration.ofSeconds(5)).map(seq -> {
            float[] serverMSPT = msptService.getServerMSPT();
            System.out.println(Arrays.toString(serverMSPT));
            return ServerSentEvent.<String>builder().id(seq.toString()).event("message").
                    data(String.format("%s,%s,%s", serverMSPT[0], serverMSPT[1], serverMSPT[2])).build();
        });
    }
}
