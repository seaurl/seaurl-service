package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiRequest {
    private String model;
    private String apiKey;
    private String baseUrl;
    private String conversationId;
    private String role;
    private String content;
    private String title;
}
