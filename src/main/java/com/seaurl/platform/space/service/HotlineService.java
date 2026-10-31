package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.Hotline;
import com.seaurl.platform.space.model.MostUrlsRank;
import com.seaurl.platform.space.model.QueryResults;
import com.seaurl.platform.common.model.Url;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface HotlineService {
    Hotline add(Hotline hotline);

    boolean edit(Hotline hotline);

    boolean delete(String id);

    List<Hotline> queryAll();
}
