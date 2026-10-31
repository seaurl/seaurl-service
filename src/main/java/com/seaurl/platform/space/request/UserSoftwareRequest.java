package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSoftwareRequest {
    private String userName;

    private String softwareId;

    private String param;

    private String groupId;
}
