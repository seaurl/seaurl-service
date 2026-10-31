package com.seaurl.platform.users.service;

import com.seaurl.platform.users.model.UserPoints;

import java.util.HashMap;

public interface UserPointsService {
    void add(UserPoints userPoints);

    boolean update(UserPoints userPoints);

    boolean delete(String uid);

    UserPoints queryBy(String uid);

    Integer queryPointBy(String uid, String date, String format);
}
