package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserFollowRequest {
    private String userName;
    private String followedUserName;
    // true关注，false取消关注
    private Boolean status;
}
