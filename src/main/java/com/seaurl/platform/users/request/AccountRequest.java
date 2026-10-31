package com.seaurl.platform.users.request;

import lombok.Data;

@Data
public class AccountRequest {
    private String userName;
    private String code;

    private String newUserName;
    private String oldPwd;
    private String newPwd;

    private String background;
    private String color;
}
