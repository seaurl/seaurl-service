package com.seaurl.platform.users.service;

import com.seaurl.platform.users.model.UserPoints;
import com.seaurl.platform.users.model.UserPointsHistory;

public interface UserPointsHistoryService {
    void add(UserPointsHistory userPointsHistory);

    boolean update(UserPointsHistory userPointsHistory);

    boolean delete(String uid);
}
