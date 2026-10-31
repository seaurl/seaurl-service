package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.UserLikeUrl;

import java.util.List;

public interface UserLikeUrlService {
    boolean update(UserLikeUrl userLikeUrl);

    void add(UserLikeUrl userLikeUrl);

    UserLikeUrl isExist(String uid, String urlId);

    List<UserLikeUrl> queryLikeUrlByUserId(String uid, List<String> urlId);
}
