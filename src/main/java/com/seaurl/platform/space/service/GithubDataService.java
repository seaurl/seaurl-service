package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.OnlineTools;
import com.seaurl.platform.space.model.QueryResults2;
import com.seaurl.platform.space.response.github.GithubData;
import com.seaurl.platform.space.response.github.Response;

import java.util.HashMap;
import java.util.List;

public interface GithubDataService {
    boolean add(GithubData githubData);

    void deleteAll();

    QueryResults2 queryBy(String param, String language, Integer pageNum, Integer pageSize);
}
