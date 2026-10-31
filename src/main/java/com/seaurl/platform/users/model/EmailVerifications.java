package com.seaurl.platform.users.model;

import lombok.Data;

import java.util.Date;

@Data
public class EmailVerifications {
    private String id;

    private String email;

    private String verificationCode;

    private Boolean isUsed;

    private Date expiresDt;

    //创建时间
    private Date createdDt;

    //更新时间
    private Date updatedDt;
}
