package com.seaurl.platform.users.service.impl;

import java.util.Arrays;
import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.common.model.PageResults;
import com.seaurl.platform.common.model.UserEvent;
import com.seaurl.platform.users.service.UserEventService;
import com.seaurl.platform.users.utils.DateUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.ArrayOperators.Filter.filter;

@Service
public class UserEventServiceImpl implements UserEventService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public void add(UserEvent userEvent) {
        mongoTemplate.insert(userEvent);
    }

    @Override
    public boolean remove(String userEventId) {
        Query query = new Query(Criteria.where("id").is(userEventId));
        DeleteResult result = mongoTemplate.remove(query, UserEvent.class);

        return result.getDeletedCount() > 0;
    }

    @Override
    public List<HashMap> queryByUserIds(String ids) {
        return null;
    }

    /**
     * 获取我关注的人的用户动态
     *
     * @param uids
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageResults getUserEvents(List<String> uids, Integer pageNum, Integer pageSize) {
        // 1、添加uid字段类型转换
        AddFieldsOperation addFieldsOperation1 = AddFieldsOperation.addField("objectId").withValue(ConvertOperators.ToObjectId.toObjectId("$objectId")).build();
        // 2、按参数顺序：被关联表url，主表userEvent.objectId，被关联表url.id，别名
        LookupOperation lookupOperation1 = Aggregation.lookup("url", "objectId", "_id", "url_as");
//        LookupOperation lookupOperation2 = Aggregation.lookup("userLikeUrl", "objectId", "urlId", "userLikeUrl_as");
        LookupOperation lookupOperation3 = Aggregation.lookup("userCopyUrl", "objectId", "fromUrlId", "userCopyUrl_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userClickUrl", "objectId", "urlId", "userClickUrl_as");
        LookupOperation lookupOperation4 = Aggregation.lookup("user", "uid", "uid", "user_as");

        Criteria query = Criteria.where("url_as.uid").in(uids).and("url_as.isDelete").is(false);
        MatchOperation matchOperation1 = Aggregation.match(query);

        ProjectionOperation projectionOperation1 = Aggregation.project("user_as.uid", "user_as.userName", "user_as.avatar", "action", "objectType", "objectOthers",
                "createdDt", "url_as", "userCopyUrl_as")
                .and("userClickUrl_as.clickCount").as("userClickUrlCount")
                .andExpression("toString(objectId)").as("objectId")
                .andExpression("toString(_id)").as("id");
                // 先筛选出userLikeUrl_as.valid=1的列表到userLikeUrl_as_list中
//                .and(filter("userLikeUrl_as").as("item")
//                        .by(ComparisonOperators.Eq.valueOf("item.valid").equalToValue(1))).as("userLikeUrl_as_list");

        ProjectionOperation projectionOperation2 = Aggregation.project("id", "uid", "userName", "avatar", "action", "objectType", "objectOthers",
                "createdDt", "url_as", "objectId", "userClickUrlCount")
                // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
//                .andExpression("{$size: '$userLikeUrl_as_list'}").as("userLikeUrl_count")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count");


        // 4、按照apple.device数量排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");

        // 添加分页操作
        AggregationOperation skip = skip((pageNum - 1) * pageSize);
        AggregationOperation limit = limit(pageSize);

        UnwindOperation unwindOperation1 = Aggregation.unwind("url_as");
        UnwindOperation unwindOperation2 = Aggregation.unwind("user_as");

        List<AggregationOperation> operationList = Arrays.asList();
        operationList.add(lookupOperation3);
        operationList.add(lookupOperation2);
        operationList.add(lookupOperation4);

        operationList.add(addFieldsOperation1);
        operationList.add(lookupOperation1);
        operationList.add(matchOperation1);

        operationList.add(unwindOperation1);
        operationList.add(unwindOperation2);

        operationList.add(projectionOperation1);
        operationList.add(projectionOperation2);

        operationList.add(sort);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<Document> totalResult = mongoTemplate.aggregate(agg, "userEvent", Document.class);
        // 计算总页数
        long totalCount = totalResult.getMappedResults().size();

        operationList.add(skip);
        operationList.add(limit);
        Aggregation agg2 = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg2, "userEvent", HashMap.class);

        List<HashMap> results = aggregationResults.getMappedResults();

        PageResults queryResults = new PageResults();
        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(totalCount);
        queryResults.setRows(results);
        return queryResults;
    }

    /**
     * 为你推荐的动态(也就是用户添加的网址动态，可以是我关注的也可以不是我关注的，总之是系统推荐的)，而且是本周内的
     * 注意：推荐列表里面不能有我的数据
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageResults getUserEvents(String uid,Integer pageNum, Integer pageSize) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();
        // 获取前7天数据
        c.setTime(new Date());
        c.add(Calendar.DATE, -7);
        Date m = c.getTime();
        String prev7day = format.format(m);
        log.info("prev7day================{}",prev7day);

        // 1、添加uid字段类型转换
        AddFieldsOperation addFieldsOperation1 = AddFieldsOperation.addField("objectId").withValue(ConvertOperators.ToObjectId.toObjectId("$objectId")).build();
        // 2、按参数顺序：被关联表url，主表userEvent.objectId，被关联表url.id，别名
        LookupOperation lookupOperation1 = Aggregation.lookup("url", "objectId", "_id", "url_as");
//        LookupOperation lookupOperation2 = Aggregation.lookup("userLikeUrl", "objectId", "urlId", "userLikeUrl_as");
        LookupOperation lookupOperation3 = Aggregation.lookup("userCopyUrl", "objectId", "fromUrlId", "userCopyUrl_as");
        LookupOperation lookupOperation2 = Aggregation.lookup("userClickUrl", "objectId", "urlId", "userClickUrl_as");
        LookupOperation lookupOperation4 = Aggregation.lookup("user", "uid", "uid", "user_as");

        // 注意：推荐列表里面不能有我的数据
        Criteria query = Criteria.where("uid").nin(uid).and("url_as.isDelete").is(false).andOperator(
                Criteria.where("createdDt").gte(
                        // 要使用ISO日期格式的才行，否则 时间会多少算8小时
                        DateUtils.dateToISODate(prev7day)
                ));
        MatchOperation matchOperation1 = Aggregation.match(query);

        ProjectionOperation projectionOperation1 = Aggregation.project("user_as.uid", "user_as.userName", "user_as.avatar", "action", "objectType", "objectOthers",
                        "createdDt", "url_as", "userCopyUrl_as")
                .and("userClickUrl_as.clickCount").as("userClickUrlCount")
                .andExpression("toString(objectId)").as("objectId")
                .andExpression("toString(_id)").as("id");
        // 先筛选出userLikeUrl_as.valid=1的列表到userLikeUrl_as_list中
//                .and(filter("userLikeUrl_as").as("item")
//                        .by(ComparisonOperators.Eq.valueOf("item.valid").equalToValue(1))).as("userLikeUrl_as_list");

        ProjectionOperation projectionOperation2 = Aggregation.project("id", "uid", "userName", "avatar", "action", "objectType", "objectOthers",
                        "createdDt", "url_as", "objectId", "userClickUrlCount")
                // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
//                .andExpression("{$size: '$userLikeUrl_as_list'}").as("userLikeUrl_count")
                .andExpression("{$size: '$userCopyUrl_as'}").as("userCopyUrl_count");


        // 4、用户点击次数、用户复制次数和添加时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt")
                .and(Sort.Direction.DESC, "userClickUrlCount")
                .and(Sort.Direction.DESC, "userCopyUrl_count");

        // 添加分页操作
        AggregationOperation skip = skip((pageNum - 1) * pageSize);
        AggregationOperation limit = limit(pageSize);

        UnwindOperation unwindOperation1 = Aggregation.unwind("url_as");
        UnwindOperation unwindOperation2 = Aggregation.unwind("user_as");

        List<AggregationOperation> operationList = Arrays.asList();
        operationList.add(lookupOperation3);
        operationList.add(lookupOperation2);
        operationList.add(lookupOperation4);

        operationList.add(addFieldsOperation1);
        operationList.add(lookupOperation1);
        operationList.add(matchOperation1);

        operationList.add(unwindOperation1);
        operationList.add(unwindOperation2);

        operationList.add(projectionOperation1);
        operationList.add(projectionOperation2);

        operationList.add(sort);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<Document> totalResult = mongoTemplate.aggregate(agg, "userEvent", Document.class);
        // 计算总页数
        long totalCount = totalResult.getMappedResults().size();

        operationList.add(skip);
        operationList.add(limit);
        Aggregation agg2 = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg2, "userEvent", HashMap.class);

        List<HashMap> results = aggregationResults.getMappedResults();

        PageResults queryResults = new PageResults();
        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(totalCount);
        queryResults.setRows(results);
        return queryResults;
    }
}
