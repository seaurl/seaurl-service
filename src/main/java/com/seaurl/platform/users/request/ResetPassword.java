package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPassword {
    private String newPwd;
    private String email;
    private String userName;
}
