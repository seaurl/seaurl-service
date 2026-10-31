package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.service.ChatAiMessageService;
import com.seaurl.platform.space.utils.DateUtils;
import com.seaurl.platform.space.utils.XDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChatAiMessageServiceImpl implements ChatAiMessageService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ChatAiMessage add(ChatAiMessage chatAiMessage) {
        ChatAiMessage chatAiMessage1 = mongoTemplate.save(chatAiMessage);
        return chatAiMessage1;
    }

    @Override
    public List<ChatAiMessage> queryMessage(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        query.with(Sort.by(Sort.Order.desc("createdDt")));
        query.limit(10);
        return mongoTemplate.find(query, ChatAiMessage.class);
    }

    @Override
    public List<ChatAiMessage> queryMessage(String uid, String type) {
        Query query = new Query(Criteria.where("uid").is(uid).and("type").is(type));
        query.with(Sort.by(Sort.Order.desc("createdDt")));
        query.limit(10);
        return mongoTemplate.find(query, ChatAiMessage.class);
    }

    @Override
    public List<ChatAiMessage> queryMessage(String uid, String type, String toolId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("type").is(type).and("aiToolId").is(toolId));
        query.with(Sort.by(Sort.Order.desc("createdDt")));
        query.limit(10);
        return mongoTemplate.find(query, ChatAiMessage.class);
    }

    @Override
    public QueryResults2 queryMessageByPage(String uid, Integer pageNum, Integer pageSize) {

        Criteria criteria1 = Criteria.where("uid").is(uid);

        Query query1 = new Query(criteria1);
        Query query2 = new Query(criteria1);
        query1.with(Sort.by(Sort.Order.desc("createdDt")));
        query1.skip(0).limit(pageSize * pageNum);

        long total = mongoTemplate.count(query2, ChatAiMessage.class);
        List<ChatAiMessage> results = mongoTemplate.find(query1, ChatAiMessage.class);

        QueryResults2<ChatAiMessage> queryResults = new QueryResults2<>();
        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }

    @Override
    public QueryResults queryChatMessageByPage(String uid, String toolId, String message, Integer pageNum, Integer pageSize) {
        Criteria criteria = Criteria.where("uid").is(uid).and("aiToolId").is(toolId);
        // 添加 message 模糊查询（如果 message 不为空）
        if (StringUtils.hasLength(message)) {
            // 使用正则表达式忽略大小写，并匹配包含内容的任意位置
            Pattern pattern = Pattern.compile(".*" + message + ".*", Pattern.CASE_INSENSITIVE);
            criteria = criteria.and("message").regex(pattern);
        }
        // 创建查询对象
        Query query = new Query(criteria);

        // 计算总数
        long total = mongoTemplate.count(query, ChatAiMessage.class);

        // 设置排序和分页参数
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.Direction.DESC, "createdDt"),
                Aggregation.skip((long) pageSize * (pageNum - 1)),
                Aggregation.limit(pageSize),
                Aggregation.project("aiToolId", "uid", "message","reasonMessage", "role", "type", "createdDt", "updatedDt")
                        .andExpression("toString(_id)").as("id")
        );

        // 执行聚合查询
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(aggregation, "chatAiMessage", HashMap.class);
        List<HashMap> results = aggregationResults.getMappedResults();
        QueryResults queryResults = new QueryResults();

        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }

    @Override
    public QueryResults queryChatMessageByPage(String uid, String role, String toolId, String message, Integer pageNum, Integer pageSize) {
        Criteria criteria = Criteria.where("uid").is(uid).and("aiToolId").is(toolId);
        // 添加 message 模糊查询（如果 message 不为空）
        if (StringUtils.hasLength(message)) {
            // 使用正则表达式忽略大小写，并匹配包含内容的任意位置
            Pattern pattern = Pattern.compile(".*" + message + ".*", Pattern.CASE_INSENSITIVE);
            criteria = criteria.and("message").regex(pattern);
        }

        // 添加 type 查询（如果 type 不为空）
        if (StringUtils.hasLength(role)) {
            criteria = criteria.and("role").is(role);
        }

        // 创建查询对象
        Query query = new Query(criteria);

        // 计算总数
        long total = mongoTemplate.count(query, ChatAiMessage.class);

        // 设置排序和分页参数
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.Direction.DESC, "createdDt"),
                Aggregation.skip((long) pageSize * (pageNum - 1)),
                Aggregation.limit(pageSize),
                Aggregation.project("aiToolId", "uid", "message","reasonMessage", "role", "type","file", "createdDt", "updatedDt")
                        .andExpression("toString(_id)").as("id")
        );

        // 执行聚合查询
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(aggregation, "chatAiMessage", HashMap.class);
        List<HashMap> results = aggregationResults.getMappedResults();
        QueryResults queryResults = new QueryResults();

        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }

    @Override
    public QueryResults queryMessageByPage(String uid, String toolId, Integer pageNum, Integer pageSize) {
        Criteria criteria = Criteria.where("uid").is(uid).and("aiToolId").is(toolId);

        // 创建查询对象
        Query query = new Query(criteria);

        // 计算总数
        long total = mongoTemplate.count(query, ChatAiMessage.class);

        // 设置排序和分页参数
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.Direction.DESC, "createdDt"),
                Aggregation.skip((long) pageSize * (pageNum - 1)),
                Aggregation.limit(pageSize),
                Aggregation.project("aiToolId", "uid", "message","reasonMessage", "role", "type", "createdDt", "updatedDt")
                        .andExpression("toString(_id)").as("id")
        );

        // 执行聚合查询
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(aggregation, "chatAiMessage", HashMap.class);
        List<HashMap> results = aggregationResults.getMappedResults();
        QueryResults queryResults = new QueryResults();

        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }

    @Override
    public QueryResults queryMessageByPage(String uid, String datetime, String toolId, Integer pageNum, Integer pageSize) {

        log.info("datetime={}", datetime);
        // 将时间转化成iso时间
        // datetime格式：年-月-日 时：分：秒
        Date d = XDateUtils.isoDateStrToDate(datetime);
        log.info("d={}", d);

        Criteria criteria = Criteria.where("uid").is(uid).and("aiToolId").is(toolId);
        criteria.and("createdDt").lte(d);

        // 创建查询对象
        Query query = new Query(criteria);

        // 计算总数
        long total = mongoTemplate.count(query, ChatAiMessage.class);

        // 设置排序和分页参数
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.Direction.DESC, "createdDt"),
                Aggregation.skip((long) pageSize * (pageNum - 1)),
                Aggregation.limit(pageSize),
                Aggregation.project( "aiToolId", "uid", "message", "reasonMessage", "role", "type", "createdDt", "updatedDt")
                        .andExpression("toString(_id)").as("id")
        );

        // 执行聚合查询
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(aggregation, "chatAiMessage", HashMap.class);
        List<HashMap> results = aggregationResults.getMappedResults();

        QueryResults queryResults = new QueryResults();
        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }

    @Override
    public boolean removeByChatAiId(String uid, String chatAiId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("chatAiId").is(chatAiId));
        DeleteResult result = mongoTemplate.remove(query, ChatAiMessage.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean removeById(String uid, String id) {
        Query query = new Query(Criteria.where("uid").is(uid).and("id").is(id));
        DeleteResult result = mongoTemplate.remove(query, ChatAiMessage.class);
        return result.getDeletedCount() > 0;
    }
}
