package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.User;
import com.seaurl.platform.common.model.UserWatch;

import java.util.HashMap;
import java.util.List;

public interface UserWatchService {
    void add(UserWatch userWatch);

    List<HashMap> getWatchUserToTodayList(String uid);
}
