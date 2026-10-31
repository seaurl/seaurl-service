package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class User {
    private String id;
    private String uid;
    private String userName;
    private String nickName;
    private String company;
    private String school;
    private String location;
    private String website;
    private String gender;
    private Long birthday;
    private String signature;
    private String introduction;
    private String email;
    private Long emailBindTime;
    private String mobile;
    private Long mobileBindTime;
    private String face;
    private String face200;
    private String avatar;
    private Integer status;
    private Integer type;
    private String backgroundImg;
    private String background = "light";
    private String color = "#54458a";
    private String ipAddr;
    private Date createdDt;
    private Date updatedDt;
    private List<String> notices;
}
