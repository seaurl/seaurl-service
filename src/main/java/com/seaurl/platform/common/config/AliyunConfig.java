package com.seaurl.platform.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class AliyunConfig {

    @Value("${aliyun.region:cn-hangzhou}")
    private String region;

    public String getRegion() {
        return region;
    }

    @Value("${aliyun.access-key-id:}")
    private String accessKeyId;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    @Value("${aliyun.access-key-secret:}")
    private String accessKeySecret;

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    @Value("${aliyun.oss-bucket:}")
    private String ossBucket;

    public String getOssBucket() {
        return ossBucket;
    }

    @Value("${aliyun.oss-bucket-region:}")
    private String ossBucketRegion;

    public String getOssBucketRegion() {
        return ossBucketRegion;
    }

    @Value("${aliyun.oss-bucket-endpoint:}")
    private String ossBucketEndpoint;

    public String getOssBucketEndpoint() {
        return ossBucketEndpoint;
    }

    @Value("${aliyun.oss-bucket-cdn-host:}")
    private String ossBucketCdnHost;

    public String getOssBucketCdnHost() {
        return ossBucketCdnHost;
    }

    @Value("${aliyun.ram-sts-endpoint:}")
    private String ramStsEndpoint;

    public String getRamStsEndpoint() {
        return ramStsEndpoint;
    }

    @Value("${aliyun.ram-access-key-id:}")
    private String ramAccessKeyId;

    public String getRamAccessKeyId() {
        return ramAccessKeyId;
    }

    @Value("${aliyun.ram-access-key-secret:}")
    private String ramAccessKeySecret;

    public String getRamAccessKeySecret() {
        return ramAccessKeySecret;
    }

    @Value("${aliyun.ram-role-arn:}")
    private String ramRoleArn;

    public String getRamRoleArn() {
        return ramRoleArn;
    }

    @Value("${aliyun.dashscope-api-key:}")
    private String dashscopeApiKey;

    public String getDashscopeApiKey() {
        return dashscopeApiKey;
    }
}
