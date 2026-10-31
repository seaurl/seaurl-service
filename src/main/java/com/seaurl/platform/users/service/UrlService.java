package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UrlService {

    Url queryById(String urlId);

    List<Url> queryUrlByCategoryId(String categoryId);

    List<Url> queryByIds(String[] ids);

    List<Url> queryUrlByTitle(String uid, String title);

    List<Url> getAllUrlsByUid(String uid);

    List<HashMap> getMyFollowingUrlList(List<String> uids, Integer pageNum, Integer pageSize);

    List<HashMap> getHeatMapSubmitDataByCurrentYear(String uid);

    List<HashMap> getHeatMapSubmitDataByYear(String uid, String year);

    List<Integer> getSubmitYearList(User user);

    List<HashMap> getUrlListBy(String uid, String date, String format, Boolean isMyself);

    List<HashMap> getUrlListBy(String uid, String date, String format);

    Integer getLikeMeUrlCount(String uid, String date, String format);

    Integer getCopyUrlCount(String uid, String date, String format);

}
