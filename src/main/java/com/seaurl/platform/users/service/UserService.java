package com.seaurl.platform.users.service;


import com.seaurl.platform.common.model.PageResults;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.common.model.UserFollow;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface UserService {
    boolean isExistUsername(String username);

    boolean isExistEmail(String email);

    boolean isExistEmailAndIsVerified(String email);

    boolean updateUserVerified(String username);

    boolean updateUserAvatar(String userName, String avatar);

    List<User> findAll();

    boolean isVerified(String username);

    User getUserByName(String username);

    List<User> getUserByName(List<String> excludeUids,String userName, Integer skipCount);

    User getUserByEmail(String email);

    Map<String, Object> getUserData(String uid);

    List<String> getFollowingUserIdList(String uid);

    List<HashMap> getFollowingList(String uid, Long pageNum, Long pageSize);

    List<HashMap> getFollowerList(String uid, Long pageNum, Long pageSize);

    List<HashMap> getRecommendedAttentionUsers(String uid);

    boolean updateUserBackgroundImg(String uid, String imgSrc);

    List<HashMap> getTop10UsersInMonth(List<String> hadFollowedUids);

    PageResults getTopUsersByPage(List<String> hadFollowedUids, Integer pageNum, Integer pageSize);
}
