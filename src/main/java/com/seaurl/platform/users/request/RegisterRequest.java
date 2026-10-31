package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String username;
    private String email;
    private String password;

    // ip归属地
    private String region;

    // aliyun 前端校验返回的
    private String captchaVerifyParam;
}
