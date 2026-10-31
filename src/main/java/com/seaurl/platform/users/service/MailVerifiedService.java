package com.seaurl.platform.users.service;


import com.seaurl.common.result.ResultInfo;
import com.seaurl.platform.users.model.MailVerified;

public interface MailVerifiedService {
    void add(MailVerified mailVerified);

    long removeByUsername(String username);

    ResultInfo verified(String username, String code);
}
