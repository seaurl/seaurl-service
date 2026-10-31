package com.seaurl.platform.space.service.impl;

import java.util.Arrays;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.service.MailMessageService;
import com.seaurl.platform.space.service.MailProviderService;
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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Aggregates.project;

@Service
public class MailMessageServiceImpl implements MailMessageService {


    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;


    @Override
    public List<MailMessage> addForList(List<MailMessage> mailMessageList) {
        return mongoTemplate.insertAll(mailMessageList).stream().toList();
    }

    @Override
    public Boolean isExist(String email, Long mid, String folderName) {
        Query query = new Query(Criteria.where("email").is(email)
                .and("mid").is(mid)
                .and("folderName").is(folderName));
        MailMessage result = mongoTemplate.findOne(query, MailMessage.class);
        return result != null;
    }

    @Override
    public List<MailMessage> queryBy(String uid, String email, String folderName) {
        Query query = Query.query(Criteria.where("uid").is(uid).and("email").is(email).and("folderName").is(folderName));
        return mongoTemplate.find(query, MailMessage.class);
    }

    @Override
    public Boolean setMessage(String uid, String messageId) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(messageId).and("uid").is(uid));
        Update update = new Update();
        update.set("isUnread", false);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, MailMessage.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Boolean removeEmail(String id) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();
        update.set("isDelete", true);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, MailMessage.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Boolean setAttachment(String uid, String email, String folderName, Long mid, String fileId, String ossUrl) {
        Date date = new Date();
        Query query = new Query(Criteria.where("email").is(email)
                .and("folderName").is(folderName)
                .and("mid").is(mid)
                .and("attachments.fileId").is(fileId));
        Update update = new Update();
        update.set("attachments.$.url", ossUrl); // 使用 $ 符号定位到符合条件的列表元素
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, MailMessage.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Boolean setAttachment(String uid, String email, String folderName, Long mid, String fileId, String ossUrl, Long fileSize) {
        Date date = new Date();
        Query query = new Query(Criteria.where("email").is(email)
                .and("folderName").is(folderName)
                .and("mid").is(mid)
                .and("attachments.fileId").is(fileId));
        Update update = new Update();
        update.set("attachments.$.url", ossUrl); // 使用 $ 符号定位到符合条件的列表元素
        update.set("attachments.$.size", fileSize); // 使用 $ 符号定位到符合条件的列表元素
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, MailMessage.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public QueryResults queryBy(String uid, String email, String folderName, Integer pageNum, Integer pageSize) {
        Criteria criteria = Criteria.where("uid").is(uid)
                .and("email").is(email)
                .and("folderName").is(folderName)
                .and("isDelete").is(false);

        // 创建查询对象
        Query query = new Query(criteria);

        // 计算总数
        long total = mongoTemplate.count(query, MailMessage.class);

        // 设置排序和分页参数
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.Direction.DESC, "receivedDate"),
                Aggregation.skip((long) pageSize * (pageNum - 1)),
                Aggregation.limit(pageSize),
                Aggregation.project("uid", "email", "mid", "folderName", "ccRecipients", "recipients", "replyTo", "from",
                                "subject", "content", "isUnread", "isFlagged", "sentDate", "receivedDate", "count", "attachments",
                                "createdDt", "updatedDt")
                        .andExpression("toString(_id)").as("id")
        );

        // 执行聚合查询
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(aggregation, MailMessage.class, HashMap.class);
        List<HashMap> results = aggregationResults.getMappedResults();
        QueryResults queryResults = new QueryResults();

        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }

    @Override
    public QueryResults queryBy(String uid, String email, String folderName, Boolean isUnread, Integer pageNum, Integer pageSize) {
        Criteria criteria = Criteria.where("uid").is(uid)
                .and("email").is(email)
                .and("folderName").is(folderName)
                .and("isUnread").is(isUnread)
                .and("isDelete").is(false);

        // 创建查询对象
        Query query = new Query(criteria);

        // 计算总数
        long total = mongoTemplate.count(query, MailMessage.class);

        // 设置排序和分页参数
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.Direction.DESC, "receivedDate"),
                Aggregation.skip((long) pageSize * (pageNum - 1)),
                Aggregation.limit(pageSize),
                Aggregation.project("uid", "email", "mid", "folderName", "ccRecipients", "recipients", "replyTo", "from",
                                "subject", "content", "isUnread", "isFlagged", "sentDate", "receivedDate", "count", "attachments",
                                "createdDt", "updatedDt")
                        .andExpression("toString(_id)").as("id")
        );

        // 执行聚合查询
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(aggregation, MailMessage.class, HashMap.class);
        List<HashMap> results = aggregationResults.getMappedResults();
        QueryResults queryResults = new QueryResults();

        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }
}
