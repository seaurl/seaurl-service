package com.seaurl.platform.space.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class DoubaoConfig {
    @Value("${doubao.access-url}")
    private String accessUrl = "";

    public String getAccessUrl() {
        return accessUrl;
    }

    @Value("${doubao.access-key-secret}")
    private String accessKeySecret = "";

    public String getAccessKeySecret() {
        return accessKeySecret;
    }
}
