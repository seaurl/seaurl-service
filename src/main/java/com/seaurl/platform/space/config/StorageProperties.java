package com.seaurl.platform.space.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {
    private String type = "local";
    private Local local = new Local();
    private Minio minio = new Minio();

    @Data
    public static class Local {
        private String basePath = "./data/uploads";
        private String baseUrl = "http://localhost:7777/api/uploads";
    }

    @Data
    public static class Minio {
        private String endpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        private String bucket = "seaurl";
    }
}
