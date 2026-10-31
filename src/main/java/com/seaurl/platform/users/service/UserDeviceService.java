package com.seaurl.platform.users.service;

import com.seaurl.platform.users.model.UserDevice;

import java.util.List;

public interface UserDeviceService {
    void add(com.seaurl.platform.users.model.UserDevice userDevice);

    boolean deleteAll(String uid);

    boolean deleteById(String tokenId);

    List<UserDevice> queryByUid(String uid);
}
