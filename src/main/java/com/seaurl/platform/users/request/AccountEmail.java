package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountEmail {
    private String userName;
    private String newEmail;

    private String email;
    private String captchaVerifyParam;
}
