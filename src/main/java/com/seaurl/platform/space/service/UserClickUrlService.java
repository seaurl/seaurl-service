package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.space.model.UserClickUrl;

import java.util.List;

public interface UserClickUrlService {

    boolean add(UserClickUrl userClickUrl);

    boolean updateClickCount(String id, int count);

    UserClickUrl queryBy(String uid, String urlId);
}
