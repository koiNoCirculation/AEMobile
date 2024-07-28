package dev.youtiao.aemobile.web;

import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
public class ServerPortCustomizer 
  implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
 
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setPort(44444);
        Compression compression = new Compression();
        compression.setEnabled(true);
        compression.setMimeTypes(new String[]{"text/html","text/css","text/javascript","application/json"});
        compression.setMinResponseSize(DataSize.ofBytes(2048));
        factory.setCompression(compression);
    }
}