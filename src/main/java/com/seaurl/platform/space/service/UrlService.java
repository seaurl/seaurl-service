package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.MostUrlsRank;
import com.seaurl.platform.space.model.QueryResults;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.UrlCategory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UrlService {
    Mono<Url> addUrl(Url url) throws IOException;

    Mono<Boolean> editUrl(Url url) throws IOException;

    Url copyUrl(Url url);

    boolean isExistUrl(String uid, String url);

    List<Url> queryUrlByCategoryId(String categoryId);

    List<HashMap> queryUrlByCategoryId(String categoryId, Integer pageNum, Integer pageSize);

    List<Url> queryIsFavoriteUrlList(String uid, Boolean isMyself);

    Object queryIsFavoriteUrlCount(String uid, Boolean isMyself);

    List<Url> queryIsTopUrlList(String uid, Boolean isMyself);

    Object queryIsTopUrlCount(String uid, Boolean isMyself);

    QueryResults queryAndSearchUrls(String uid, String categoryId, String content, String sortType, Boolean isMyself, Integer pageNum, Integer pageSize);

    QueryResults queryAndSearchUrls(String uid, String categoryId, String date, String format, String sortType, Boolean isMyself, Integer pageNum, Integer pageSize);

    boolean deleteUrl(String urlId);

    List<Url> queryUrlByTitle(String uid, String title);

    HashMap queryUrlByTitle(String uid, String title, Integer pageNum, Integer pageSize);

    HashMap queryUrlByTitle(String categoryId, String uid, String title, Integer pageNum, Integer pageSize);

    boolean updateAttention(String urlId, boolean isAttention);

    Map<String, Object> getUrlsForLatestMonth(String uid);

    List<HashMap> getUrlsForLatestMonth(String uid, String flag);
    List<HashMap> getUrlsForLatestMonth(String uid, String flag, Boolean isMySelf);

    List<HashMap> getUserClickUrls(String uid, Boolean isMyself);

    Object getUserClickUrlsCount(String uid, Boolean isMyself);

    List<HashMap> getUserHistoryUrls(String uid, Boolean isMyself);

    Object getUserHistoryUrlsCount(String uid, Boolean isMyself);

    List<Url> getAttentionUrl(String uid);

    List<Url> getFastCopyUrl(String uid);

    List<Url> getCollectionUrl(String uid);

    Url queryUrlById(String urlId);

    List<MostUrlsRank> mostUrlsRank(String uid);

    List<HashMap> top10LikeUrlsInWeek();

    List<HashMap> top10ClickUrlsInWeek();

    boolean setTop(String urlId, Boolean isTop);

    boolean setFavorite(String urlId, boolean isFavorite);
}
