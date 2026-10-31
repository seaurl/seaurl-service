package com.seaurl.platform.space.request;

import lombok.Data;

@Data
public class TranslatorRequest {
    private String sourceTxt;

    private String sourceLng;

    private String targetLng;
}
