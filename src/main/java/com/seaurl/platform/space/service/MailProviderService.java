package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.MailProvider;

import java.util.List;

public interface MailProviderService {
    MailProvider add(MailProvider mailProvider);

    Boolean update(MailProvider mailProvider);

    Boolean updateStatus(String email, String uid, String status);

    Boolean delete(String id);

    boolean create(MailProvider mailProvider);

    boolean isExistEmail(String uid, String email);

    boolean isExistName(String uid, String name);

    List<MailProvider> queryAll(String uid);

    MailProvider queryBy(String email, String uid);

    MailProvider queryBy(String email);
}
