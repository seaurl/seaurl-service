package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.UserCommonUrls;
import com.seaurl.platform.space.model.UserTools;

import java.util.HashMap;
import java.util.List;

public interface UserCommonUrlsService {

    boolean add(UserCommonUrls userCommonUrls);

    boolean edit(UserCommonUrls userCommonUrls);

    boolean remove(String uid, String groupId);

    List<UserCommonUrls> queryByUid(String uid);

    List<HashMap> queryUserCommonUrls(String uid);

    UserCommonUrls queryBy(String uid, String groupId);
}
