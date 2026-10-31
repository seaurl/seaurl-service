package com.seaurl.platform.space.service.impl;

import java.util.Arrays;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.service.*;
import com.seaurl.platform.space.service.client.HttpDownloadWebClient;
import com.seaurl.platform.space.utils.DateUtils;
import com.seaurl.platform.space.utils.Random;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.ArrayOperators.Filter.filter;

@Service
public class UrlServiceImpl implements UrlService {
    private static Logger log = LoggerFactory.getLogger(UrlServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ReptileService reptileService;

    @Autowired
    StorageService storageService;

    @Autowired
    DomainService domainService;

    @Autowired
    HttpDownloadWebClient httpDownloadService;

    // 不要使用google.com，使用google.cn
    String googleHelp = "https://www.google.cn/s2/favicons?domain=";

    // 抓取失败时的默认 favicon（仍走 OSS，前端 onError fallback 也硬编码此地址）
    private static final String DEFAULT_FAVICON = "https://cdn.seaurl.com/space/url/www/default.png";

    /**
     * 添加网址
     *
     * @param url
     * @return
     * @throws IOException
     */
    @Override
    public Mono<Url> addUrl(Url url) throws IOException {
        // 1、获取url中的域名
        String domainName = reptileService.getDomain(url.getUrl());
        log.info("domainName={}", domainName);
        //2、判断是否域名有值
        if (!StringUtils.hasLength(domainName)) {
            return Mono.empty();
        }

        url.setDomain(domainName);
        if (!StringUtils.hasLength(url.getKeywords())){
            url.setKeywords(url.getKeywords());
        }
        // 3、判断域名是否存在于Domain表中，
        Domain queryDomain = domainService.queryDomainByName(domainName);
        // 如果存在domain,则从domain表中直接获取并赋值，免去查询的必要
        if (queryDomain != null && queryDomain.getFavicon() != null && StringUtils.hasLength(queryDomain.getFavicon())) {
            //4、如果存在，直接从Domain表中拿icon，
            url.setFavicon(queryDomain.getFavicon());
            // 7、添加Url数据，返回true
            url.setIsDelete(false);
            // 添加提交网址结束
            return Mono.fromCallable(() -> mongoTemplate.insert(url));
        }
        // 5、否则，获取favicon URL（不下载图片，直接存原站 URL）
        HashMap content = reptileService.getFaviconFromUrl(url.getUrl());
        if (content == null || !StringUtils.hasLength((String) content.get("faviconUrl"))) {
            url.setIsDelete(false);
            url.setFavicon(DEFAULT_FAVICON);
            return Mono.fromCallable(() -> mongoTemplate.insert(url));
        }
        String faviconUrl = (String) content.get("faviconUrl");

        Domain domain = new Domain();
        domain.setFavicon(faviconUrl);
        domain.setName(domainName);
        domain.setLightHexColor("rgb(" + Random.getRandom(200, 255) + "," + Random.getRandom(200, 255) + "," + Random.getRandom(200, 255) + ")");
        domain.setIsPublic(true);
        domain.setCreatedDt(new Date());
        domain.setUpdatedDt(new Date());

        // 6、添加Domain表数据
        domainService.addDomain(domain);
        // 7、添加Url数据，返回true
        url.setIsDelete(false);
        url.setFavicon(faviconUrl);
        log.info("-----------------favicon url:{}", url.getFavicon());
        return Mono.fromCallable(() -> mongoTemplate.insert(url));
    }

    @Override
    public Mono<Boolean> editUrl(Url url) throws IOException {

        Query query = new Query(Criteria.where("id").is(url.getId()));
        Update update = new Update();
        update.set("title", url.getTitle());
//        update.set("favicon", url.getFavicon());
        update.set("description", url.getDescription());
        update.set("keywords", url.getKeywords());
        update.set("categoryId", url.getCategoryId());
        update.set("isFastCopy", false);
        update.set("isPrivate", url.getIsPrivate());
        update.set("updatedDt", new Date());

        Url oldUrl = queryUrlById(url.getId());
        // 获取改动之前的url的domain
        Domain queryDomain = domainService.queryDomainByName(oldUrl.getDomain());
        // 1、获取url中的域名
//        String domainName = reptileService.getDomain(url.getUrl());
        // 如果存在domain,则从domain表中直接获取并赋值，免去查询的必要
        if (queryDomain != null && queryDomain.getFavicon() != null && StringUtils.hasLength(queryDomain.getFavicon())) {
            //4、如果存在，直接从Domain表中拿icon，
            update.set("favicon", queryDomain.getFavicon());
            UpdateResult result = mongoTemplate.updateFirst(query, update, Url.class);
            return Mono.fromCallable(() -> result.getModifiedCount() > 0);
        }
        // 5、否则，获取favicon URL（不下载图片，直接存原站 URL）
        HashMap content = reptileService.getFaviconFromUrl(oldUrl.getUrl());
        if (content == null || !StringUtils.hasLength((String) content.get("faviconUrl"))) {
            update.set("favicon", DEFAULT_FAVICON);
            UpdateResult result = mongoTemplate.updateFirst(query, update, Url.class);
            return Mono.fromCallable(() -> result.getModifiedCount() > 0);
        }
        String faviconUrl = (String) content.get("faviconUrl");

        Domain domain = new Domain();
        domain.setFavicon(faviconUrl);
        update.set("favicon", faviconUrl);
        domain.setName(oldUrl.getDomain());
        domain.setIsPublic(true);
        domain.setCreatedDt(new Date());
        domain.setUpdatedDt(new Date());
        // 6、添加Domain表数据
        domainService.addDomain(domain);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Url.class);
        return Mono.fromCallable(() -> result.getModifiedCount() > 0);
    }

    /**
     * 复制网址
     *
     * @param url
     * @return
     */
    @Override
    public Url copyUrl(Url url) {
        Url urlCopyed = mongoTemplate.insert(url);
        // 添加提交网址结束
        return urlCopyed;
    }

    @Override
    public boolean isExistUrl(String uid, String url) {
        Query query = new Query(Criteria.where("uid").is(uid).and("url").is(url).and("isDelete").is(false));
        List<Url> urls = mongoTemplate.find(query, Url.class);
        return urls.size() > 0;
    }

    @Override
    public List<Url> queryUrlByCategoryId(String categoryId) {
        Query query = new Query(Criteria.where("categoryId").is(categoryId).and("isDelete").is(false));
        query.with(Sort.by(Sort.Order.desc("createdDt")));
        return mongoTemplate.find(query, Url.class);
    }

    @Override
    public List<HashMap> queryUrlByCategoryId(String categoryId, Integer pageNum, Integer pageSize) {
        AddFieldsOperation addFieldsOperation1 = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();

        LookupOperation lookupOperation2 = Aggregation.lookup("userLikeUrl", "_id", "urlId", "userLikeUrl_as");
        LookupOperation lookupOperation3 = Aggregation.lookup("userCopyUrl", "_id", "fromUrlId", "userCopyUrl_as");
        LookupOperation lookupOperation4 = Aggregation.lookup("userClickUrl", "_id", "urlId", "userClickUrl_as");
        MatchOperation matchOperation1 = Aggregation.match(Criteria.where("categoryId").is(categoryId).and("isDelete").is(false));
        UnwindOperation unwindOperation1 = Aggregation.unwind("clickCount");
        ProjectionOperation projectionOperation1 = Aggregation.project("uid", "categoryId", "title", "url", "domain", "favicon",
                        "isCopy", "isTop", "isDelete", "createdDt", "updatedDt", "userCopyUrl_as")
                .andExpression("_id").as("id")
                // 先筛选出userLikeUrl_as.valid=1的列表到userLikeUrl_as_list中
                .and(filter("userLikeUrl_as").as("item")
                        .by(ComparisonOperators.Eq.valueOf("item.valid").equalToValue(1))).as("userLikeUrl_as_list")
                .and("userClickUrl_as.clickCount").as("clickCount");

        ProjectionOperation projectionOperation2 = Aggregation.project("id", "uid", "categoryId", "title", "url", "domain", "favicon",
                        "isCopy", "isTop", "createdDt", "updatedDt", "userCopyUrl_as", "clickCount")
                // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
                .andExpression("{$size: '$userLikeUrl_as_list'}").as("userLikeUrl_count")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count");


        // 4、按照apple.device数量排序
        SortOperation sort = Aggregation.sort(Sort.by(Sort.Order.desc("isTop"), Sort.Order.desc("updatedDt")));

        FacetOperation facet = Aggregation.facet(
                        count().as("count")).as("total")
                .and(skip(pageSize * (pageNum - 1)), limit(pageSize)).as("rows");

//        UnwindOperation unwindOperation1 = Aggregation.unwind("url_as");

        List<AggregationOperation> operationList = new ArrayList<>();
        operationList.add(addFieldsOperation1);

        operationList.add(lookupOperation3);
        operationList.add(lookupOperation2);
        operationList.add(lookupOperation4);
        operationList.add(matchOperation1);

        operationList.add(projectionOperation1);
        operationList.add(projectionOperation2);
//        operationList.add(unwindOperation1);

        operationList.add(sort);
        operationList.add(facet);

        Aggregation agg = Aggregation.newAggregation(operationList);
        // 因为返回的是User实体类，所以不会出现device字段，你可以使用Map.class来返回想要的字段
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "url", HashMap.class);
        return aggregationResults.getMappedResults();
    }

    @Override
    public List<Url> queryIsFavoriteUrlList(String uid, Boolean isMyself) {

        Criteria criteria = new Criteria("uid").is(uid).and("isDelete").is(false).and("isFavorite").is(true);
        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        query.limit(60); // 限制返回前 20 条数据

        return mongoTemplate.find(query, Url.class);
    }

    @Override
    public Object queryIsFavoriteUrlCount(String uid, Boolean isMyself) {
        Criteria criteria = new Criteria("uid").is(uid).and("isDelete").is(false).and("isFavorite").is(true);
        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }
        Query query = new Query(criteria);

        return mongoTemplate.count(query, Url.class);
    }

    @Override
    public List<Url> queryIsTopUrlList(String uid, Boolean isMyself) {
        Criteria criteria = Criteria.where("uid").is(uid).and("isDelete").is(false).and("isTop").is(true);
        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        query.limit(60); // 限制返回前 20 条数据

        return mongoTemplate.find(query, Url.class);
    }

    @Override
    public Object queryIsTopUrlCount(String uid, Boolean isMyself) {
        Criteria criteria = Criteria.where("uid").is(uid).and("isDelete").is(false).and("isTop").is(true);
        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }
        Query query = new Query(criteria);
        return mongoTemplate.count(query, Url.class);
    }

    @Override
    public QueryResults queryAndSearchUrls(String uid, String categoryId, String content, String sortType, Boolean isMyself, Integer pageNum, Integer pageSize) {
        AddFieldsOperation addFieldsOperation1 = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();

//        LookupOperation lookupOperation2 = Aggregation.lookup("userLikeUrl", "_id", "urlId", "userLikeUrl_as");
        LookupOperation lookupOperation3 = Aggregation.lookup("userCopyUrl", "_id", "fromUrlId", "userCopyUrl_as");
        LookupOperation lookupOperation4 = Aggregation.lookup("userClickUrl", "_id", "urlId", "userClickUrl_as");
        LookupOperation lookupOperation5 = Aggregation.lookup("domain", "domain", "name", "domain_as");

        Criteria criteria = new Criteria();
        Criteria criteria1 = new Criteria("isDelete").is(false);
        Criteria criteria2 = new Criteria("isDelete").is(false);
        if (StringUtils.hasLength(categoryId)) {
            criteria1.and("categoryId").is(categoryId);
            criteria2.and("categoryId").is(categoryId);
        }
        if (StringUtils.hasLength(content)) {
            Pattern pattern = Pattern.compile("^.*" + content + ".*$", Pattern.CASE_INSENSITIVE);
            criteria1.and("uid").is(uid).and("title").regex(pattern);
            criteria2.and("uid").is(uid).and("domain").regex(pattern);
        }

        if (!isMyself) {
            criteria1.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
            criteria2.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        criteria.orOperator(criteria1, criteria2);
        MatchOperation matchOperation1 = Aggregation.match(criteria);

        ProjectionOperation projectionOperation1 = Aggregation.project("uid", "categoryId", "title", "description", "keywords", "url", "domain", "favicon",
                        "isFavorite", "isCopy", "isTop", "isDelete", "isPrivate", "createdDt", "updatedDt", "userCopyUrl_as", "domain_as")
                .andExpression("_id").as("id")
                // 先筛选出userLikeUrl_as.valid=1的列表到userLikeUrl_as_list中
//                .and(filter("userLikeUrl_as").as("item")
//                        .by(ComparisonOperators.Eq.valueOf("item.valid").equalToValue(1))).as("userLikeUrl_as_list")
                .and("userClickUrl_as.clickCount").as("clickCount");

        ProjectionOperation projectionOperation2 = Aggregation.project("id", "uid", "categoryId", "title", "description", "keywords", "url", "domain", "favicon",
                        "isFavorite", "isCopy", "isTop", "isPrivate", "createdDt", "updatedDt", "clickCount", "domain_as")
                // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
//                .andExpression("{$size: '$userLikeUrl_as_list'}").as("userLikeUrl_count")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count");


        // 4、按照apple.device数量排序
        SortOperation sort = null;
        if (sortType.equals("comprehensive")) {
            sort = Aggregation.sort(Sort.by(Sort.Order.desc("isTop"),
                    Sort.Order.desc("clickCount"),
                    Sort.Order.desc("userCopyUrl_count"),
                    Sort.Order.desc("createdDt"))
            );
        }
        if (sortType.equals("upLike")) {
            sort = Aggregation.sort(Sort.by(
                    Sort.Order.desc("clickCount"))
            );
        }
        if (sortType.equals("upCopy")) {
            sort = Aggregation.sort(Sort.by(
                    Sort.Order.desc("userCopyUrl_count"))
            );
        }
        if (sortType.equals("created")) {
            sort = Aggregation.sort(Sort.by(
                    Sort.Order.desc("createdDt"))
            );
        }
        SkipOperation skipOperation = skip(pageSize * (pageNum - 1));
        LimitOperation limitOperation = limit(pageSize);

        List<AggregationOperation> operationList = new ArrayList<>();
        operationList.add(addFieldsOperation1);

        operationList.add(lookupOperation3);
//        operationList.add(lookupOperation2);
        operationList.add(lookupOperation4);
        operationList.add(lookupOperation5);
        operationList.add(matchOperation1);

        operationList.add(projectionOperation1);
        operationList.add(projectionOperation2);

        operationList.add(sort);

        Aggregation agg = Aggregation.newAggregation(operationList);
        // 因为返回的是User实体类，所以不会出现device字段，你可以使用Map.class来返回想要的字段
//        AggregationResults<QueryResults> aggregationResults = mongoTemplate.aggregate(agg, "url", QueryResults.class);
        AggregationResults<Document> totalResult = mongoTemplate.aggregate(agg, "url", Document.class);
        // 计算总页数
        long totalCount = totalResult.getMappedResults().size();

        operationList.add(skipOperation);
        operationList.add(limitOperation);
        Aggregation agg2 = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg2, "url", HashMap.class);

        List<HashMap> results = aggregationResults.getMappedResults();

        QueryResults queryResults = new QueryResults();
        queryResults.setPageNum(pageNum + 1);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(totalCount);
        queryResults.setRows(results);
        return queryResults;
    }

    @Override
    public QueryResults queryAndSearchUrls(String uid, String categoryId, String date, String format, String sortType, Boolean isMyself, Integer pageNum, Integer pageSize) {
        AddFieldsOperation addFieldsOperation1 = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();

//        LookupOperation lookupOperation2 = Aggregation.lookup("userLikeUrl", "_id", "urlId", "userLikeUrl_as");
        LookupOperation lookupOperation3 = Aggregation.lookup("userCopyUrl", "_id", "fromUrlId", "userCopyUrl_as");
        LookupOperation lookupOperation4 = Aggregation.lookup("userClickUrl", "_id", "urlId", "userClickUrl_as");
        LookupOperation lookupOperation5 = Aggregation.lookup("domain", "domain", "name", "domain_as");

        Criteria criteria = new Criteria("isDelete").is(false).and("uid").is(uid);

        if (StringUtils.hasLength(categoryId)) {
            criteria.and("categoryId").is(categoryId);
        }

        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        MatchOperation matchOperation1 = Aggregation.match(criteria);
        MatchOperation matchOperation2 = Aggregation.match(Criteria.where("date").is(date));

        ProjectionOperation projectionOperation1 = Aggregation.project("uid", "categoryId", "title", "description", "keywords", "url", "domain", "favicon",
                        "isFavorite", "isCopy", "isTop", "isDelete", "isPrivate", "createdDt", "updatedDt", "userCopyUrl_as", "domain_as")
                .andExpression("_id").as("id")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '" + format + "', timezone: '+08:00'}}").as("date")

                // 先筛选出userLikeUrl_as.valid=1的列表到userLikeUrl_as_list中
//                .and(filter("userLikeUrl_as").as("item")
//                        .by(ComparisonOperators.Eq.valueOf("item.valid").equalToValue(1))).as("userLikeUrl_as_list")
                .and("userClickUrl_as.clickCount").as("clickCount");

        ProjectionOperation projectionOperation2 = Aggregation.project("id", "uid", "categoryId", "title", "description", "keywords", "url", "domain", "favicon",
                        "isFavorite", "isCopy", "isTop", "isPrivate", "createdDt", "updatedDt", "clickCount", "date", "domain_as")
                // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
//                .andExpression("{$size: '$userLikeUrl_as_list'}").as("userLikeUrl_count")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count");


        // 4、按照apple.device数量排序
        SortOperation sort = null;
        if (sortType.equals("comprehensive")) {
            sort = Aggregation.sort(Sort.by(Sort.Order.desc("isTop"),
                    Sort.Order.desc("clickCount"),
                    Sort.Order.desc("userCopyUrl_count"),
                    Sort.Order.desc("createdDt"))
            );
        }
        if (sortType.equals("upLike")) {
            sort = Aggregation.sort(Sort.by(
                    Sort.Order.desc("clickCount"))
            );
        }
        if (sortType.equals("upCopy")) {
            sort = Aggregation.sort(Sort.by(
                    Sort.Order.desc("userCopyUrl_count"))
            );
        }
        if (sortType.equals("created")) {
            sort = Aggregation.sort(Sort.by(
                    Sort.Order.desc("createdDt"))
            );
        }

        SkipOperation skipOperation = skip(pageSize * (pageNum - 1));
        LimitOperation limitOperation = limit(pageSize);

        List<AggregationOperation> operationList = new ArrayList<>();
        operationList.add(addFieldsOperation1);

        operationList.add(lookupOperation3);
//        operationList.add(lookupOperation2);
        operationList.add(lookupOperation4);
        operationList.add(lookupOperation5);
        operationList.add(matchOperation1);

        operationList.add(projectionOperation1);
        operationList.add(projectionOperation2);
        operationList.add(matchOperation2);

        operationList.add(sort);

        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<Document> totalResult = mongoTemplate.aggregate(agg, "url", Document.class);
        // 计算总页数
        long totalCount = totalResult.getMappedResults().size();

        operationList.add(skipOperation);
        operationList.add(limitOperation);
        Aggregation agg2 = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg2, "url", HashMap.class);

        List<HashMap> results = aggregationResults.getMappedResults();

        QueryResults queryResults = new QueryResults();
        queryResults.setPageNum(pageNum + 1);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(totalCount);
        queryResults.setRows(results);
        return queryResults;
    }

    /**
     * 删除网址
     *
     * @param urlId
     * @return
     */
    @Override
    public boolean deleteUrl(String urlId) {
        Query query = new Query(Criteria.where("id").is(urlId));
        Update update = new Update();
        update.set("isDelete", true);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Url.class);

        try {
            // --- 添加提交网址结束 ---
            return result.getModifiedCount() > 0;
//        DeleteResult deleteResult = mongoTemplate.remove(query, Url.class);
//        return deleteResult.getDeletedCount() > 0;
        } catch (Exception e) {
            log.info("exception msg={}", e.getMessage());
            return false;
        }

    }

    @Override
    public List<Url> queryUrlByTitle(String uid, String title) {
        // 模糊匹配
        Pattern pattern = Pattern.compile("^.*" + title + ".*$", Pattern.CASE_INSENSITIVE);
        Criteria criteria1 = Criteria.where("uid").is(uid).and("title").regex(pattern).and("isDelete").is(false);
        Criteria criteria2 = Criteria.where("uid").is(uid).and("domain").regex(pattern).and("isDelete").is(false);
        Query query = new Query(new Criteria().orOperator(criteria1, criteria2));
        return mongoTemplate.find(query, Url.class);
    }

    @Override
    public HashMap queryUrlByTitle(String uid, String title, Integer pageNum, Integer pageSize) {
        // 模糊匹配
        Pattern pattern = Pattern.compile("^.*" + title + ".*$", Pattern.CASE_INSENSITIVE);
        Criteria criteria1 = Criteria.where("uid").is(uid).and("title").regex(pattern).and("isDelete").is(false);
        Criteria criteria2 = Criteria.where("uid").is(uid).and("domain").regex(pattern).and("isDelete").is(false);

        Query query = new Query(new Criteria().orOperator(criteria1, criteria2));
        query.with(Sort.by(Sort.Order.desc("isTop"), Sort.Order.desc("updatedDt")));
        query.skip((long) pageSize * (pageNum - 1)).limit(pageSize);

        Long total = mongoTemplate.count(new Query(new Criteria().orOperator(criteria1, criteria2)), Url.class);
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("rows", mongoTemplate.find(query, Url.class));
        return map;
    }

    @Override
    public HashMap queryUrlByTitle(String categoryId, String uid, String title, Integer pageNum, Integer pageSize) {
        // 模糊匹配
        Pattern pattern = Pattern.compile("^.*" + title + ".*$", Pattern.CASE_INSENSITIVE);
        Criteria criteria1 = Criteria.where("uid").is(uid).and("title").regex(pattern).and("isDelete").is(false).and("categoryId").is(categoryId);
        Criteria criteria2 = Criteria.where("uid").is(uid).and("domain").regex(pattern).and("isDelete").is(false).and("categoryId").is(categoryId);

        Query query = new Query(new Criteria().orOperator(criteria1, criteria2));
        query.with(Sort.by(Sort.Order.desc("isTop"), Sort.Order.desc("updatedDt")));
        query.skip((long) pageSize * (pageNum - 1)).limit(pageSize);

        Long total = mongoTemplate.count(new Query(new Criteria().orOperator(criteria1, criteria2)), Url.class);
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("rows", mongoTemplate.find(query, Url.class));
        return map;
    }

    @Override
    public boolean updateAttention(String urlId, boolean isAttention) {
        Query query = new Query(Criteria.where("id").is(urlId));
        Update update = new Update();
        update.set("isAttention", isAttention);
        update.set("attentionDt", new Date());
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Url.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Map<String, Object> getUrlsForLatestMonth(String uid) {
        Map<String, Object> map = new HashMap<>();
        List<HashMap> recentlyAddedOrCopyUrls = getRecentlyAddUrls(uid);
        List<HashMap> recentlyVisitUrls = getClickUrls(uid);
//        List<Url> recentlyCopyUrls = getCopyUrls(uid);
        // 1、获取最近一个月添加和复制的网址
        map.put("recentlyAddUrls", recentlyAddedOrCopyUrls);
        // 2、获取经常访问的网址
        map.put("recentlyVisitUrls", recentlyVisitUrls);
        // 3、获取最近复制的网址(暂时不获取了，因为上面的查询一个月数据里面已经包含了复制的网址，如果获取放开即可)
//        map.put("recentlyCopyUrls", recentlyCopyUrls);
        return map;
    }

    /**
     * @param uid
     * @param flag
     * @return
     */
    @Override
    public List<HashMap> getUrlsForLatestMonth(String uid, String flag) {
        Map<String, Object> map = new HashMap<>();
//        List<Url> recentlyCopyUrls = getCopyUrls(uid);
        if (flag.equals("add")) {
            // 1、获取最近一个月添加和复制的网址
            return getRecentlyAddUrls(uid);
        }
        if (flag.equals("like")) {
            // 1、获取最近一个月添加和复制的网址
            return getLikeUrls(uid);
        }
        if (flag.equals("likeMe")) {
            // 1、获取最近一个月添加和复制的网址
            return getLikeMeUrls(uid);
        }
        if (flag.equals("click")) {
            // 2、获取经常访问的网址
            return getClickUrls(uid);
        }
        if (flag.equals("copy")) {
            // 2、获取经常访问的网址
            return getCopyUrls(uid);
        }
        return null;
        // 3、获取最近复制的网址(暂时不获取了，因为上面的查询一个月数据里面已经包含了复制的网址，如果获取放开即可)
//        map.put("recentlyCopyUrls", recentlyCopyUrls);
    }

    @Override
    public List<HashMap> getUrlsForLatestMonth(String uid, String flag, Boolean isMySelf) {
        Map<String, Object> map = new HashMap<>();
//        List<Url> recentlyCopyUrls = getCopyUrls(uid);
        if (flag.equals("add")) {
            // 1、获取最近一个月添加和复制的网址
            return getRecentlyAddUrls(uid, isMySelf);
        }
        if (flag.equals("like")) {
            // 1、获取最近一个月添加和复制的网址
            return getLikeUrls(uid, isMySelf);
        }
        if (flag.equals("likeMe")) {
            // 1、获取最近一个月添加和复制的网址
            return getLikeMeUrls(uid, isMySelf);
        }
        if (flag.equals("click")) {
            // 2、获取经常访问的网址
            return getClickUrls(uid, isMySelf);
        }
        if (flag.equals("copy")) {
            // 2、获取经常访问的网址
            return getCopyUrls(uid, isMySelf);
        }
        return null;
        // 3、获取最近复制的网址(暂时不获取了，因为上面的查询一个月数据里面已经包含了复制的网址，如果获取放开即可)
//        map.put("recentlyCopyUrls", recentlyCopyUrls);
    }

    @Override
    public List<HashMap> getUserClickUrls(String uid, Boolean isMyself) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userCopyUrl", "urlId", "fromUrlId", "userCopyUrl_as");
        Criteria criteria = new Criteria("uid").is(uid).and("url_as.isDelete").is(false);
        // 如果是别人，则查询我所有公开的网址
        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        AggregationOperation match = Aggregation.match(criteria);


        ProjectionOperation project = Aggregation.project("url_as.isTop", "url_as.isFavorite", "url_as.isPrivate", "url_as.uid", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "url_as.createdDt")
                .andInclude("clickCount")
                .andExpression("toString(url_as._id)").as("id")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date");

        UnwindOperation unwindOperation = Aggregation.unwind("url_as");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "clickCount");
        LimitOperation limit = Aggregation.limit(60);
        operationList.add(lookupOperation2);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);
        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, UserClickUrl.class, HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    @Override
    public Object getUserClickUrlsCount(String uid, Boolean isMyself) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userCopyUrl", "urlId", "fromUrlId", "userCopyUrl_as");
        Criteria criteria = new Criteria("uid").is(uid).and("url_as.isDelete").is(false);
        // 如果是别人，则查询我所有公开的网址
        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        AggregationOperation match = Aggregation.match(criteria);


        ProjectionOperation project = Aggregation.project("url_as.isTop", "url_as.isFavorite", "url_as.isPrivate", "url_as.uid", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "url_as.createdDt")
                .andInclude("clickCount")
                .andExpression("toString(url_as._id)").as("id")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date");

        UnwindOperation unwindOperation = Aggregation.unwind("url_as");
        // sort里面的clickCount字段必须从Project中取，否则 失败
//        LimitOperation limit = Aggregation.limit(60);
        operationList.add(lookupOperation2);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        // 只需要计数，不需要投影和限制
        operationList.add(Aggregation.count().as("count"));
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, UserClickUrl.class, HashMap.class);
        return Optional.ofNullable(aggregationResults.getUniqueMappedResult())
                .map(result -> result.get("count"))
                .orElse(0);

    }

    @Override
    public List<HashMap> getUserHistoryUrls(String uid, Boolean isMyself) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");

        Criteria criteria = new Criteria("uid").is(uid).and("url_as.isDelete").is(false);
        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        AggregationOperation match = Aggregation.match(criteria);

        ProjectionOperation project = Aggregation.project("url_as.isTop", "url_as.isFavorite", "url_as.isPrivate", "url_as.uid", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "url_as.createdDt")
                .andExpression("toString(url_as._id)").as("id")
                .andExpression("{ $dateToString: {date: '$updatedDt', format: '%Y-%m-%d %H:%M:%S', timezone: '+08:00'}}").as("date");

        UnwindOperation unwindOperation = Aggregation.unwind("url_as");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "date");
        LimitOperation limit = Aggregation.limit(60);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);
        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, UserClickUrl.class, HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    @Override
    public Object getUserHistoryUrlsCount(String uid, Boolean isMyself) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");

        Criteria criteria = new Criteria("uid").is(uid).and("url_as.isDelete").is(false);
        if (!isMyself) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        AggregationOperation match = Aggregation.match(criteria);

        ProjectionOperation project = Aggregation.project("url_as.isTop", "url_as.isFavorite", "url_as.isPrivate", "url_as.uid", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "url_as.createdDt")
                .andExpression("toString(url_as._id)").as("id")
                .andExpression("{ $dateToString: {date: '$updatedDt', format: '%Y-%m-%d %H:%M:%S', timezone: '+08:00'}}").as("date");

        UnwindOperation unwindOperation = Aggregation.unwind("url_as");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "date");
        LimitOperation limit = Aggregation.limit(60);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(Aggregation.count().as("count"));
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, UserClickUrl.class, HashMap.class);
        return Optional.ofNullable(aggregationResults.getUniqueMappedResult())
                .map(result -> result.get("count"))
                .orElse(0);

    }

    //获取复制的网址
    private List<HashMap> getCopyUrls(String uid) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
//        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("toUrlId").withValue(ConvertOperators.ToObjectId.toObjectId("$toUrlId")).build();
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("userCopyUrl", "_id", "fromUrlId", "userCopyUrl_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userClickUrl", "_id", "urlId", "userClickUrl_as");

        AggregationOperation match = Aggregation.match(Criteria.where("uid").is(uid).and("isDelete").is(false).and("isCopy").is(true));
        ProjectionOperation project = Aggregation.project("uid", "categoryId", "title", "url", "domain", "favicon", "createdDt", "userClickUrl_as.clickCount")
                .and("_id").as("id")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count");
        ;
        LimitOperation limit = Aggregation.limit(60);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        UnwindOperation unwind = Aggregation.unwind("url_as");

        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(lookupOperation2);

        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
//        operationList.add(unwind);
        operationList.add(sort);

        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "url", HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    private List<HashMap> getCopyUrls(String uid, Boolean isMySelf) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
//        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("toUrlId").withValue(ConvertOperators.ToObjectId.toObjectId("$toUrlId")).build();
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("userCopyUrl", "_id", "fromUrlId", "userCopyUrl_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userClickUrl", "_id", "urlId", "userClickUrl_as");

        Criteria criteria = Criteria.where("uid").is(uid).and("isDelete").is(false).and("isCopy").is(true);

        if (!isMySelf) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        AggregationOperation match = Aggregation.match(criteria);
        ProjectionOperation project = Aggregation.project("uid", "categoryId", "title", "url", "domain", "favicon", "isPrivate", "createdDt", "userClickUrl_as.clickCount")
                .and("_id").as("id")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count");
        ;
        LimitOperation limit = Aggregation.limit(60);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        UnwindOperation unwind = Aggregation.unwind("url_as");

        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(lookupOperation2);

        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
//        operationList.add(unwind);
        operationList.add(sort);

        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "url", HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    //获取我点赞的网址
    private List<HashMap> getLikeUrls(String uid) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userClickUrl", "urlId", "urlId", "userClickUrl_as");

        UnwindOperation unwind = Aggregation.unwind("url_as");
        AggregationOperation match = Aggregation.match(Criteria.where("uid").is(uid).and("url_as.isDelete").is(false).and("valid").is(1));

        ProjectionOperation project = Aggregation.project("url_as.uid", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "createdDt", "updatedDt", "userClickUrl_as.clickCount")
                .andExpression("toString(_id)").as("id");
        LimitOperation limit = Aggregation.limit(60);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt");

        // 先关联相同类型的id表
        operationList.add(lookupOperation2);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwind);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);

        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userLikeUrl", HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    private List<HashMap> getLikeUrls(String uid, Boolean isMySelf) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userClickUrl", "urlId", "urlId", "userClickUrl_as");

        UnwindOperation unwind = Aggregation.unwind("url_as");

        Criteria criteria = Criteria.where("uid").is(uid).and("url_as.isDelete").is(false).and("valid").is(1);

        if (!isMySelf) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("url_as.isPrivate").is("1"),
                            Criteria.where("url_as.isPrivate").exists(false)
                    )
            );
        }

        AggregationOperation match = Aggregation.match(criteria);

        ProjectionOperation project = Aggregation.project("url_as.uid", "url_as.isPrivate", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "createdDt", "updatedDt", "userClickUrl_as.clickCount")
                .andExpression("toString(_id)").as("id");
        LimitOperation limit = Aggregation.limit(60);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt");

        // 先关联相同类型的id表
        operationList.add(lookupOperation2);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwind);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);

        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userLikeUrl", HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    /**
     * 查询获赞的网址
     *
     * @param uid
     * @return
     */
    private List<HashMap> getLikeMeUrls(String uid) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userClickUrl", "urlId", "urlId", "userClickUrl_as");

        UnwindOperation unwind = Aggregation.unwind("url_as");
        AggregationOperation match = Aggregation.match(Criteria.where("url_as.uid").is(uid)
                .and("url_as.isDelete").is(false)
                .and("valid").is(1)
                // 忽略自己点赞的，查询的应该是别人点赞我的
                .and("uid").ne(uid));

        ProjectionOperation project = Aggregation.project("url_as.uid", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "createdDt", "updatedDt", "userClickUrl_as.clickCount");
        LimitOperation limit = Aggregation.limit(60);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt");
        // 先关联相同类型的id表
        operationList.add(lookupOperation2);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwind);
        operationList.add(match);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(project);
        operationList.add(sort);

        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userLikeUrl", HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    private List<HashMap> getLikeMeUrls(String uid, Boolean isMySelf) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userClickUrl", "urlId", "urlId", "userClickUrl_as");

        UnwindOperation unwind = Aggregation.unwind("url_as");

        Criteria criteria = Criteria.where("url_as.uid").is(uid)
                .and("url_as.isDelete").is(false)
                .and("valid").is(1)
                // 忽略自己点赞的，查询的应该是别人点赞我的
                .and("uid").ne(uid);

        if (!isMySelf) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("url_as.isPrivate").is("1"),
                            Criteria.where("url_as.isPrivate").exists(false)
                    )
            );
        }

        AggregationOperation match = Aggregation.match(criteria);

        ProjectionOperation project = Aggregation.project("url_as.uid", "url_as.isPrivate", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "createdDt", "updatedDt", "userClickUrl_as.clickCount");
        LimitOperation limit = Aggregation.limit(60);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt");
        // 先关联相同类型的id表
        operationList.add(lookupOperation2);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwind);
        operationList.add(match);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(project);
        operationList.add(sort);

        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userLikeUrl", HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    /**
     * 获取最近添加的网址
     *
     * @param uid
     * @return
     */
    private List<HashMap> getRecentlyAddUrls(String uid) {
        log.info("------getRecentlyAddUrls------{}", uid);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();

        // 获取上个月数据
        c.setTime(new Date());
        c.add(Calendar.MONTH, -1);
        Date m = c.getTime();
        String mon = format.format(m);

        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();
        LookupOperation lookupOperation = Aggregation.lookup("userClickUrl", "_id", "urlId", "userClickUrl_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userCopyUrl", "_id", "fromUrlId", "userCopyUrl_as");

        ProjectionOperation projectionOperation = Aggregation.project("id", "uid", "categoryId", "title", "description", "url", "domain", "favicon", "createdDt", "userClickUrl_as.clickCount")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count")
                .andExpression("toString(_id)").as("id");

        MatchOperation matchOperation = Aggregation.match(Criteria.where("uid").is(uid).and("isDelete").is(false).and("isCopy").is(false).andOperator(Criteria.where("createdDt").gte(
                // 要使用ISO日期格式的才行，否则 时间会多少算8小时
                DateUtils.dateToISODate(mon)
        )));
        LimitOperation limit = Aggregation.limit(60);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        Aggregation agg = Aggregation.newAggregation(addFieldsOperation, lookupOperation, lookupOperation2, matchOperation, projectionOperation, sort, limit);
        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, Url.class, HashMap.class);
        List<HashMap> dataList = outputType.getMappedResults();
        return dataList;
    }

    private List<HashMap> getRecentlyAddUrls(String uid, Boolean isMySelf) {
        log.info("------getRecentlyAddUrls------{}", uid);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();

        // 获取上个月数据
        c.setTime(new Date());
        c.add(Calendar.MONTH, -1);
        Date m = c.getTime();
        String mon = format.format(m);

        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();
        LookupOperation lookupOperation = Aggregation.lookup("userClickUrl", "_id", "urlId", "userClickUrl_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userCopyUrl", "_id", "fromUrlId", "userCopyUrl_as");

        ProjectionOperation projectionOperation = Aggregation.project("id", "uid", "categoryId", "title", "description", "url", "domain", "favicon", "isPrivate", "createdDt", "userClickUrl_as.clickCount")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count")
                .andExpression("toString(_id)").as("id");

        Criteria criteria = Criteria.where("uid").is(uid).and("isDelete").is(false).and("isCopy").is(false).andOperator(Criteria.where("createdDt").gte(
                // 要使用ISO日期格式的才行，否则 时间会多少算8小时
                DateUtils.dateToISODate(mon)
        ));

        if (!isMySelf) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        MatchOperation matchOperation = Aggregation.match(criteria);
        LimitOperation limit = Aggregation.limit(60);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        Aggregation agg = Aggregation.newAggregation(addFieldsOperation, lookupOperation, lookupOperation2, matchOperation, projectionOperation, sort, limit);
        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, Url.class, HashMap.class);
        List<HashMap> dataList = outputType.getMappedResults();
        return dataList;
    }

    // 获取最近访问数最多的网址
    private List<HashMap> getClickUrls(String uid) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userCopyUrl", "urlId", "fromUrlId", "userCopyUrl_as");

        AggregationOperation match = Aggregation.match(Criteria.where("uid").is(uid).and("url_as.isDelete").is(false));

        ProjectionOperation project = Aggregation.project("url_as.uid", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "url_as.createdDt")
                .andInclude("clickCount")
                .andExpression("toString(url_as._id)").as("id")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date");

        UnwindOperation unwindOperation = Aggregation.unwind("url_as");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "clickCount");
        LimitOperation limit = Aggregation.limit(60);
        operationList.add(lookupOperation2);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);
        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, UserClickUrl.class, HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    private List<HashMap> getClickUrls(String uid, Boolean isMySelf) {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "urlId", "_id", "url_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userCopyUrl", "urlId", "fromUrlId", "userCopyUrl_as");

        Criteria criteria = Criteria.where("uid").is(uid).and("url_as.isDelete").is(false);

        if (!isMySelf) {
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("url_as.isPrivate").is("1"),
                            Criteria.where("url_as.isPrivate").exists(false)
                    )
            );
        }

        AggregationOperation match = Aggregation.match(criteria);

        ProjectionOperation project = Aggregation.project("url_as.uid", "url_as.isPrivate", "url_as.categoryId", "url_as.title", "url_as.description", "url_as.url", "url_as.domain", "url_as.favicon", "url_as.createdDt")
                .andInclude("clickCount")
                .andExpression("toString(url_as._id)").as("id")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date");

        UnwindOperation unwindOperation = Aggregation.unwind("url_as");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "clickCount");
        LimitOperation limit = Aggregation.limit(60);
        operationList.add(lookupOperation2);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);
        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, UserClickUrl.class, HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    @Override
    public List<Url> getAttentionUrl(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false).and("isAttention").is(true));
        query.with(Sort.by(Sort.Order.desc("attentionDt")));
        return mongoTemplate.find(query, Url.class);
    }

    @Override
    public List<Url> getFastCopyUrl(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false).and("isFastCopy").is(true));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, Url.class);
    }

    @Override
    public List<Url> getCollectionUrl(String uid) {
        return null;
    }

    @Override
    public Url queryUrlById(String urlId) {
        Query query = new Query(Criteria.where("id").is(urlId));
        return mongoTemplate.findOne(query, Url.class);
    }

    @Override
    public List<MostUrlsRank> mostUrlsRank(String uid) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("uid").is(uid)),//条件
                // 千万不要加project，因为加了之后MostUrlsRank实体类就返回不了favicon字段，说明只有group才是真正要返回的字段
//                Aggregation.project("domain", "favicon", "count"),
                Aggregation.group("domain").count().as("count")
                        .first("favicon").as("favicon")
                        .first("createdDt").as("createdDt")
                        .first("domain").as("domain"),
                Aggregation.sort(Sort.Direction.DESC, "count").and(Sort.Direction.DESC, "createdDt"),//排序
                Aggregation.limit(10)//页数
        );
        AggregationResults<MostUrlsRank> outputType = mongoTemplate.aggregate(agg, "url", MostUrlsRank.class);
        List<MostUrlsRank> list = outputType.getMappedResults();
        return list;
    }

    /**
     * 获取用户喜欢的网址top10
     *
     * @return
     */
    @Override
    public List<HashMap> top10LikeUrlsInWeek() {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
//        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();

        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("userLikeUrl", "_id", "urlId", "userLikeUrl_as");
//        // 当前时间
//        String currentDate = XDateUtils.getCurrentDate();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();
        // 获取前7天数据
        c.setTime(new Date());
        c.add(Calendar.DATE, -7);
        Date m = c.getTime();
        String prev7day = format.format(m);

        AggregationOperation match = Aggregation.match(Criteria.where("isDelete").is(false).andOperator(
                Criteria.where("createdDt").gte(
                        // 要使用ISO日期格式的才行，否则 时间会多少算8小时
                        DateUtils.dateToISODate(prev7day)
                ))
        );
        ProjectionOperation project = Aggregation.project("title", "url", "domain", "favicon", "createdDt", "isDelete")
                .and(filter("userLikeUrl_as").as("item")
                        .by(ComparisonOperators.Eq.valueOf("item.valid").equalToValue(1))).as("userLikeUrl_as_list")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date");

        ProjectionOperation project2 = Aggregation.project("title", "url", "domain", "favicon", "createdDt")
                // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
                .andExpression("{$size: '$userLikeUrl_as_list'}").as("userLikeUrl_count");
//        UnwindOperation unwindOperation = Aggregation.unwind("url_as");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "userLikeUrl_count");
        LimitOperation limit = Aggregation.limit(10);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
//        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(project2);
        operationList.add(sort);
        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, Url.class, HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    @Override
    public List<HashMap> top10ClickUrlsInWeek() {
        List<AggregationOperation> operationList = new ArrayList<>();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
//        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("urlId").withValue(ConvertOperators.ToObjectId.toObjectId("$urlId")).build();
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();

        // url_as是子表别名，后面的操作涉及到子表的时候会用到
//        LookupOperation lookupOperation = Aggregation.lookup("userLikeUrl", "_id", "urlId", "userLikeUrl_as");
        LookupOperation lookupOperation = Aggregation.lookup("userClickUrl", "_id", "urlId", "userClickUrl_as");
//        // 当前时间
//        String currentDate = XDateUtils.getCurrentDate();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();
        // 获取前7天数据
        c.setTime(new Date());
        c.add(Calendar.DATE, -7);
        Date m = c.getTime();
        String prev7day = format.format(m);

        AggregationOperation match = Aggregation.match(Criteria.where("isDelete").is(false).andOperator(
                Criteria.where("createdDt").gte(
                        // 要使用ISO日期格式的才行，否则 时间会多少算8小时
                        DateUtils.dateToISODate(prev7day)
                ))
        );
        ProjectionOperation project = Aggregation.project("title", "url", "domain", "favicon", "createdDt", "isDelete")
                .and("userClickUrl_as.clickCount").arrayElementAt(0).as("userClickUrlCount")
//                .and(filter("userLikeUrl_as").as("item")
//                        .by(ComparisonOperators.Eq.valueOf("item.valid").equalToValue(1))).as("userLikeUrl_as_list")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date");

        ProjectionOperation project2 = Aggregation.project("title", "url", "domain", "favicon", "createdDt", "userClickUrlCount");

        // 查询大于0的数据
        AggregationOperation match2 = Aggregation.match(Criteria.where("userClickUrlCount").gt(0));

        // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
//                .andExpression("{$size: '$userLikeUrl_as_list'}").as("userLikeUrl_count");
//        UnwindOperation unwindOperation = Aggregation.unwind("url_as");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "userClickUrlCount");
        LimitOperation limit = Aggregation.limit(10);
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
//        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(project2);
        operationList.add(match2);
        operationList.add(sort);
        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, Url.class, HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    @Override
    public boolean setTop(String urlId, Boolean isTop) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(urlId));
        Update update = new Update();
        update.set("isTop", isTop);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, Url.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean setFavorite(String urlId, boolean isFavorite) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(urlId));
        Update update = new Update();
        update.set("isFavorite", isFavorite);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, Url.class);
        return result.getModifiedCount() > 0;
    }
}
