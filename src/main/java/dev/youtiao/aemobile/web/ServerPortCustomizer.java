package dev.youtiao.aemobile.web;

import dev.youtiao.aemobile.AEMobile;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.io.File;

@Component
public class ServerPortCustomizer 
  implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
 
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setPort(AEMobile.port);
        Compression compression = new Compression();
        compression.setEnabled(true);
        compression.setMimeTypes(new String[]{"text/html","text/css","text/javascript","application/json"});
        compression.setMinResponseSize(DataSize.ofBytes(2048));
        factory.setCompression(compression);
        if(AEMobile.useSSL && new File(AEMobile.certificate).exists()) {
            configSSL(factory);
        }
    }

    void configSSL(ConfigurableWebServerFactory factory) {
        Ssl ssl =new Ssl();
        ssl.setEnabled(true);
        ssl.setCertificate(AEMobile.certificate);
        ssl.setCertificatePrivateKey(AEMobile.privkey);
        factory.setSsl(ssl);
    }
}