package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.OnlineTools;
import com.seaurl.platform.space.model.OnlineToolsGroup;
import com.seaurl.platform.space.model.UserTools;

import java.util.List;

public interface OnlineToolsService {
    boolean add(OnlineTools onlineTools);

    boolean remove(String uid, String toolId);

    List<OnlineTools> queryById(String uid);

    UserTools queryBy(String uid, String toolId);

    List<OnlineToolsGroup> queryAll();

    List<OnlineTools> queryByParam(String toolGroupId, String param);
    List<OnlineTools> queryByParam(String uid,String toolGroupId, String param);
}
