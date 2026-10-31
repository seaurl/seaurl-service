package com.seaurl.platform.users.service.impl;

import java.util.Arrays;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.service.UrlService;
import com.seaurl.platform.users.utils.DateUtils;
import com.seaurl.platform.users.utils.XDateUtils;
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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service("usersUrlServiceImpl")
public class UrlServiceImpl implements UrlService {
    private static Logger log = LoggerFactory.getLogger(UrlServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public Url queryById(String urlId) {
        Query query = new Query(Criteria.where("id").is(urlId));
        return mongoTemplate.findOne(query, Url.class);
    }

    @Override
    public List<Url> queryUrlByCategoryId(String categoryId) {
        Query query = new Query(Criteria.where("categoryId").is(categoryId).and("isDelete").is(false));
        query.with(Sort.by(Sort.Order.desc("createdDt")));
        return mongoTemplate.find(query, Url.class);
    }

    @Override
    public List<Url> queryByIds(String[] ids) {
        Query query = new Query(Criteria.where("id").in(ids).and("isDelete").is(false));
        return mongoTemplate.find(query, Url.class);
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
    public List<Url> getAllUrlsByUid(String uid) {
        Query query = new Query(Criteria.where("uid").in(uid).and("isDelete").is(false));
        return mongoTemplate.find(query, Url.class);
    }

    @Override
    public List<HashMap> getMyFollowingUrlList(List<String> uids, Integer pageNum, Integer pageSize) {
        List<AggregationOperation> operationList = new ArrayList<>();
        AddFieldsOperation add1FieldsOperation = AddFieldsOperation.addField("categoryId").withValue(ConvertOperators.ToObjectId.toObjectId("$categoryId")).build();

        // 三表关联：url、urlCategory、user，
        // 主表是：url，关联表是urlCategory、user
        // foreignField是url._id=urlCategory.categoryId
        // foreignField是url.uid=user.uid
        LookupOperation lookupOperation1 = Aggregation.lookup("urlCategory", "categoryId", "_id", "category_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("user", "uid", "uid", "user_as");

        MatchOperation match = Aggregation.match(Criteria.where("uid").in(uids).and("isDelete").is(false));

        ProjectionOperation project = Aggregation.project("user_as.userName", "user_as.avatar", "uid", "title", "url", "domain", "favicon", "isAttention", "isCopy", "createdDt")
                .and("category_as.name").as("categoryName")
                .and("category_as.pid").as("categoryPid")
                .and(ConvertOperators.Convert.convertValueOf("_id").to("string")).as("id")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d %H:%M:%S', timezone: '+08:00'}}").as("date")
                .and(ConvertOperators.Convert.convertValueOf("categoryId").to("string")).as("category_id");

        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        FacetOperation facet = Aggregation.facet(
                        count().as("count")).as("total")
                .and(skip(pageSize * (pageNum - 1)), limit(pageSize)).as("rows");
        UnwindOperation unwindOperation1 = Aggregation.unwind("category_as");
        UnwindOperation unwindOperation2 = Aggregation.unwind("user_as");
        operationList.add(add1FieldsOperation);
        operationList.add(lookupOperation1);
        operationList.add(lookupOperation2);
        operationList.add(unwindOperation1);
        operationList.add(unwindOperation2);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);
        operationList.add(facet);

        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "url", HashMap.class);
        return aggregationResults.getMappedResults();
    }

    @Override
    public List<HashMap> getHeatMapSubmitDataByCurrentYear(String uid) {
        String beforeDate = XDateUtils.beforeDate();
        String currentDate = XDateUtils.getCurrentDate();

        MatchOperation matchOperation = Aggregation.match(Criteria.where("uid").is(uid).andOperator(
                Criteria.where("createdDt").gte(DateUtils.dateToISODate(beforeDate + " 00:00:00")),
                Criteria.where("createdDt").lte(DateUtils.dateToISODate(currentDate + " 23:59:59"))));

        // 千万不要加project，因为加了之后MostUrlsRank实体类就返回不了favicon字段，说明只有group才是真正要返回的字段
        ProjectionOperation projectionOperation1 = Aggregation.project("createdDt")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date");

        GroupOperation groupOperation = Aggregation.group("date").count().as("count")
                .first("createdDt").as("createdDt")
                .first("date").as("date");
        // 重新组合要返回的字段
        ProjectionOperation projectionOperation2 = Aggregation.project("createdDt", "count", "date").and("date").previousOperation();
        SortOperation sortOperation = Aggregation.sort(Sort.Direction.ASC, "date");
        Aggregation agg = Aggregation.newAggregation(matchOperation, projectionOperation1, groupOperation, projectionOperation2, sortOperation);
        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, "url", HashMap.class);
        List<HashMap> list = outputType.getMappedResults();
        return list;
    }

    @Override
    public List<HashMap> getHeatMapSubmitDataByYear(String uid, String year) {
        MatchOperation matchOperation = Aggregation.match(Criteria.where("uid").is(uid).and("year").is(year));

        ProjectionOperation projectionOperation1 = Aggregation.project("createdDt", "uid")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y', timezone: '+08:00'}}").as("year");

        GroupOperation groupOperation = Aggregation.group(fields().and("date")).count().as("count")
                .first("createdDt").as("createdDt")
                .first("uid").as("uid")
                .first("year").as("year");

        ProjectionOperation projectionOperation2 = Aggregation.project("createdDt", "count", "date", "uid", "year").and("date").previousOperation();
        SortOperation sortOperation = Aggregation.sort(Sort.Direction.ASC, "date");
        Aggregation agg = Aggregation.newAggregation(projectionOperation1, matchOperation, groupOperation, projectionOperation2, sortOperation);
        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, "url", HashMap.class);
        List<HashMap> list = outputType.getMappedResults();
        return list;
    }

    @Override
    public List<Integer> getSubmitYearList(User user) {
        Integer currentYear = XDateUtils.getCurrentYear();
        try {
            // 1、获取用户注册年份，2、将用户注册年份转换成北京时区时间
            Date date = DateUtils.formatIsoTimeZoneToBeijingTimeZone(user.getCreatedDt());
            Integer registerYear = XDateUtils.formatDateToYear(date);
            List<Integer> yearList = new ArrayList<>();
            for (int year = registerYear; year <= currentYear; year++) {
                yearList.add(year);
            }
            return yearList;
        } catch (Exception ex) {
            log.info("getSubmitYearList error={}", ex.getMessage());
            return null;
        }
    }

    @Override
    public List<HashMap> getUrlListBy(String uid, String date, String format, Boolean isMyself) {
        // 格式化日期
        String dateFormat = "";

        if (format.equals("y")) {
            dateFormat = "%Y";
        }
        if (format.equals("y-m")) {
            dateFormat = "%Y-%m";
        }
        if (format.equals("y-m-d")) {
            dateFormat = "%Y-%m-%d";
        }
        log.info("date={}", date);
        log.info("dateFormat={}", dateFormat);

        ProjectionOperation projectionOperation = Aggregation.project("createdDt", "uid", "categoryId", "title", "url", "domain", "favicon", "isCopy", "isAttention", "isDelete", "isPrivate")
                .andExpression("toString(_id)").as("id")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '" + dateFormat + "', timezone: '+08:00'}}").as("date");

        Criteria criteria = Criteria.where("uid").is(uid).and("isDelete").is(false).and("date").is(date);

        if (!isMyself){
            criteria.andOperator(
                    new Criteria().orOperator(
                            Criteria.where("isPrivate").is("1"),
                            Criteria.where("isPrivate").exists(false)
                    )
            );
        }

        MatchOperation matchOperation = Aggregation.match(criteria);



        SortOperation sortOperation = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        // 先计算总数，再限制返回数据
        FacetOperation facetOperation = Aggregation.facet(count().as("total")).as("count").and(limit(10)).as("rows");
        Aggregation agg = Aggregation.newAggregation(projectionOperation, matchOperation, sortOperation, facetOperation);

        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, "url", HashMap.class);
        return outputType.getMappedResults();
    }

    @Override
    public List<HashMap> getUrlListBy(String uid, String date, String format) {
        // 格式化日期
        String dateFormat = "";

        if (format.equals("y")) {
            dateFormat = "%Y";
        }
        if (format.equals("y-m")) {
            dateFormat = "%Y-%m";
        }
        if (format.equals("y-m-d")) {
            dateFormat = "%Y-%m-%d";
        }
        log.info("date={}", date);
        log.info("dateFormat={}", dateFormat);

        ProjectionOperation projectionOperation = Aggregation.project("createdDt", "uid", "categoryId", "title", "url", "domain", "favicon", "isCopy", "isAttention", "isDelete")
                .andExpression("toString(_id)").as("id")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '" + dateFormat + "', timezone: '+08:00'}}").as("date");

        MatchOperation matchOperation = Aggregation.match(Criteria.where("uid").is(uid).and("isDelete").is(false).and("date").is(date));
        SortOperation sortOperation = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        // 先计算总数，再限制返回数据
        FacetOperation facetOperation = Aggregation.facet(count().as("total")).as("count").and(limit(10)).as("rows");
        Aggregation agg = Aggregation.newAggregation(projectionOperation, matchOperation, sortOperation, facetOperation);

        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, "url", HashMap.class);
        return outputType.getMappedResults();
    }


    @Override
    public Integer getLikeMeUrlCount(String uid, String date, String format) {
        // 格式化日期
        String dateFormat = "";

        if (format.equals("y")) {
            dateFormat = "%Y";
        }
        if (format.equals("y-m")) {
            dateFormat = "%Y-%m";
        }
        if (format.equals("y-m-d")) {
            dateFormat = "%Y-%m-%d";
        }

        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();
        LookupOperation lookupOperation = Aggregation.lookup("userLikeUrl", "_id", "urlId", "userLikeUrl_as");

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid).and("isDelete").is(false)
                .and("date").is(date)
                // 判断like是否有效
                .and("valid").is(1)
                // 不能把自己点赞自己的数据也包含进去，否则就没意义了
                .and("neUid").ne(uid));

        ProjectionOperation project = Aggregation.project("uid","isDelete")
                .andExpression("{ $dateToString: {date: '$userLikeUrl_as.createdDt', format: '" + dateFormat + "', timezone: '+08:00'}}").as("date")
                .and("userLikeUrl_as.valid").as("valid")
                .and("userLikeUrl_as.uid").as("neUid");

        UnwindOperation unwind = Aggregation.unwind("userLikeUrl_as");

        List<AggregationOperation> operationList = new ArrayList<>();
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(unwind);
        operationList.add(project);
        operationList.add(match);
        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "url", HashMap.class);
        return aggregationResults.getMappedResults().size();
    }

    @Override
    public Integer getCopyUrlCount(String uid, String date, String format) {
        // 格式化日期
        String dateFormat = "";

        if (format.equals("y")) {
            dateFormat = "%Y";
        }
        if (format.equals("y-m")) {
            dateFormat = "%Y-%m";
        }
        if (format.equals("y-m-d")) {
            dateFormat = "%Y-%m-%d";
        }

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid)
                .and("isDelete").is(false)
                .and("isCopy").is(true)
                .and("date").is(date));

        ProjectionOperation project = Aggregation.project("uid","isDelete", "isCopy")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '" + dateFormat + "', timezone: '+08:00'}}").as("date");


        List<AggregationOperation> operationList = new ArrayList<>();
        operationList.add(project);
        operationList.add(match);
        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "url", HashMap.class);
        return aggregationResults.getMappedResults().size();
    }
}
