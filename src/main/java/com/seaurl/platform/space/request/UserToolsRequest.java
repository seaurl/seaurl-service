package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserToolsRequest {
    private String userName;
    private String toolId;
    private String groupId;
    private String param;
    // system:系统的,user:用户的
    private String type;
}
