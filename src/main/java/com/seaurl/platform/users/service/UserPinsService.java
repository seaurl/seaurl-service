package com.seaurl.platform.users.service;

import com.seaurl.platform.users.model.UserPins;

public interface UserPinsService {
    UserPins add(UserPins userPins);

    boolean update(UserPins userPins);

    UserPins queryByUid(String uid);
}
