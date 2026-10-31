package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.UserFollow;

import java.util.List;

public interface UserFollowService {

    boolean follwedUser(UserFollow userFollow);

    boolean isFollowed(String uid, String followedUserId);

    List<UserFollow> getMyFollowUserList(String uid);
}
