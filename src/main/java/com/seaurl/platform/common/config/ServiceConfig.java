package com.seaurl.platform.common.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@Getter
public class ServiceConfig {

    @Value("${signing.key:}")
    private String jwtSigningKey;

    @Value("${web.host:}")
    private String host;

    @Value("${client.id:}")
    private String clientId;

    @Value("${client.secret:}")
    private String clientSecret;

    @Value("${cookie.domain:}")
    private String domain;

    @Value("${server.env:dev}")
    private String serverEnv;

    @Value("${weather.free-private-key:}")
    private String freePrivateKey;

    @Value("${weather.free-project-id:}")
    private String freeProjectId;

    @Value("${weather.free-credent-id:}")
    private String freeCredentId;

    @Value("${weather.standard-private-key:}")
    private String standardPrivateKey;

    @Value("${weather.standard-project-id:}")
    private String standardProjectId;

    @Value("${weather.standard-credent-id:}")
    private String standardCredentId;
}
