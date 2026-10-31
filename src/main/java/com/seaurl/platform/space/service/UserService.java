package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.User;

import java.util.HashMap;
import java.util.Map;

public interface UserService {

    User getSpaceUserInfo(String userName);

    Map<String, Object> getStatisticalData(String uid);
}
