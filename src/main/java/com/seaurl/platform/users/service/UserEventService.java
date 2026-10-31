package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.PageResults;
import com.seaurl.platform.common.model.UserEvent;

import java.util.HashMap;
import java.util.List;

public interface UserEventService {
    void add(UserEvent userEvent);

    boolean remove(String userEventId);

    /**
     * 查询包含ids所有的用户动态数据
     *
     * @param ids
     * @return
     */
    List<HashMap> queryByUserIds(String ids);

    //    @Aggregation(pipeline = {"sdffsdf"})
    PageResults getUserEvents(List<String> uids, Integer pageNum, Integer pageSize);

    PageResults getUserEvents(String uid, Integer pageNum, Integer pageSize);
}
