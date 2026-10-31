package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MailProvider {
    private String id;

    private String uid;

    // qq 126 gmail
    private String provider;

    private String name;

    private String nickName;

    private String email;

    private String password;

    // imap pop
    private String protocol;

    private List<MailFolder> mailFolderList;

    // ------------------收---------------------

    private String imapHost;
    private String imapPort;
    private Boolean imapSslEnable;

    private String popHost;
    private String popPort;
    private Boolean popSslEnable;

    // ------------------发、收---------------------
    private String exchangeHost;

    // ------------------发---------------------
    private String smtpHost;
    private String smtpPort;
    private Boolean smtpSslEnable;

    // 上次同步时间
    private Date lastSync;

    // 同步状态，0：未同步，1：同步完成，2：正在同步,-1：同步失败
    private String status;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
