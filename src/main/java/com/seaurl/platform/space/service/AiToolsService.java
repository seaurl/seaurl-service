package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.AiTools;
import com.seaurl.platform.space.model.UserAiTools;

import java.util.HashMap;
import java.util.List;

public interface AiToolsService {
    AiTools add(AiTools aiTools);

    boolean delete(String id);
    boolean delete(String uid, String id);

    boolean update(AiTools aiTools);

    List<HashMap> queryAll(String uid);

    HashMap queryByUid(String uid);

    AiTools query(String id);

    List<HashMap> queryAll();
}
