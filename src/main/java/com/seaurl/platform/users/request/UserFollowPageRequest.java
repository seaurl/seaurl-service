package com.seaurl.platform.users.request;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserFollowPageRequest {

    // 当前登录的用户名
    private String loginUserName;
    // 当前访问的用户名
    private String userName;

    private Long pageSize;
    private Long pageNum;
    private String type;
}
