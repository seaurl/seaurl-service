package com.seaurl.platform.space.request;

import lombok.Data;

@Data
public class ChatAiCategoryRequest {
    private String cid;

    private String title;

    private Boolean isTop;
}
