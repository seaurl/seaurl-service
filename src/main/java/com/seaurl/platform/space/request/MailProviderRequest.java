package com.seaurl.platform.space.request;

import lombok.Data;

import java.util.Date;

@Data
public class MailProviderRequest {
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
}
