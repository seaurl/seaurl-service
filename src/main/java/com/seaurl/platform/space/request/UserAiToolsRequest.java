package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAiToolsRequest {
    private String aiToolId;

    private String userAiToolId;

    private Boolean isTop;
}
