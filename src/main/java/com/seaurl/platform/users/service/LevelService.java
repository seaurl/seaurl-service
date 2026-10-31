package com.seaurl.platform.users.service;

import com.seaurl.platform.users.model.Level;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.model.UserPoints;

import java.util.List;
import java.util.Map;

public interface LevelService {

    Level getLevelByPoints(int points);

    Level getLevelByUid(String uid);

    List<Map> getRankListBy(String param);

    List<Map> getAllRankList();
}
