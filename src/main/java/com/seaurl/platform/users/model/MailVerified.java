package com.seaurl.platform.users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 用户邮箱验证
 */
@Getter
@Setter
public class MailVerified {
    private String id;
    private String userName;
    private String mail;
    private String code;
    private Date createdDt;

    public MailVerified(String userName, String mail, String code) {
        this.userName = userName;
        this.mail = mail;
        this.code = code;
        Date now = new Date();
        this.createdDt = now;
    }
}
