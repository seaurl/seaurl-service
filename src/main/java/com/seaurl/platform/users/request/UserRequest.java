package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRequest {
    private String userName;

    private String imgSrc;

    private String nickName;

    private String school;

    private String company;

    private String website;

    private String location;

    private String signature;

    private String introduction;

    private String backgroundImg;
}
