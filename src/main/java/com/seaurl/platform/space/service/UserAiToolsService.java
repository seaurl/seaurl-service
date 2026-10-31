package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.UserAiTools;

import java.util.HashMap;
import java.util.List;

public interface UserAiToolsService {
    UserAiTools add(UserAiTools userAiTools);

    boolean delete(String usedId);

    boolean update(UserAiTools userAiTools);

    boolean updateDt(String usedId);

    boolean setTop(UserAiTools userAiTools);

    List<UserAiTools> query(UserAiTools userAiTools);


    List<HashMap> queryBy(String uid);

    List<UserAiTools> queryByUsedId(String usedId);

    List<UserAiTools> queryBy(String uid, String aiToolId);

}
