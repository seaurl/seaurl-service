package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadAvatarRequest {
    private String avatar;
    private String userName;
}
