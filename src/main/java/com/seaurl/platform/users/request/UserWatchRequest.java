package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserWatchRequest {
    // 登录用户
    private String userName;
    // 被查看的用户
    private String watchUserName;
}
