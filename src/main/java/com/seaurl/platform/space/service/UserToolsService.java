package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.space.model.UserTools;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public interface UserToolsService {
    boolean addAll(List<UserTools> userToolsList);

    boolean add(UserTools userTools);

    boolean edit(UserTools userTools);

    boolean remove(String uid, String toolId);

    List<UserTools> queryByUid(String uid);

    List<HashMap> queryUserTools(String uid);

    UserTools queryBy(String uid, String toolId);
}
