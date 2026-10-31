package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserSubmitRequest {
    private String userName;
    private String loginUserName;
    private String date;
    private String type;
    private String format;
}
