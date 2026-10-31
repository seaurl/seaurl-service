package com.seaurl.platform.common.model;

import lombok.Data;

@Data
public class LoginUser {
    private String uid;
    private String userName;
    private String email;
    private String avatar;
    private String mobile;
}
