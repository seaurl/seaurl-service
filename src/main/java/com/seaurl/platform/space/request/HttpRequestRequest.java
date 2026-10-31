package com.seaurl.platform.space.request;

import lombok.Data;

@Data
public class HttpRequestRequest {
    private String id;

    private String method;

    private String url;

    private String headers;

//    private String params;

    private String body;

    private String contentType;
}
