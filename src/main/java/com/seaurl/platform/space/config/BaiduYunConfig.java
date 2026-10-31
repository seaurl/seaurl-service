package com.seaurl.platform.space.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class BaiduYunConfig {

    @Value("${baiduyun.access-key-id}")
    private String accessKeyId = "";

    public String getAccessKeyId() {
        return accessKeyId;
    }

    @Value("${baiduyun.access-key-secret}")
    private String accessKeySecret = "";

    public String getAccessKeySecret() {
        return accessKeySecret;
    }
}
