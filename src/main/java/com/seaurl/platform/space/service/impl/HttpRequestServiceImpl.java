package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.HttpRequest;
import com.seaurl.platform.space.service.HttpRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class HttpRequestServiceImpl implements HttpRequestService {

    private Logger log = LoggerFactory.getLogger(getClass());


    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public HttpRequest add(HttpRequest httpRequest) {
        // 插入新文档（自动生成 ObjectId）
        return mongoTemplate.insert(httpRequest); // 指定集合名称
    }

    @Override
    public Boolean update(HttpRequest httpRequest) {
        // 1. 验证输入
        if (httpRequest == null || httpRequest.getId() == null) {
            throw new IllegalArgumentException("HttpRequest or its ID cannot be null");
        }

        // 2. 创建查询条件（根据ID）
        Query query = new Query(Criteria.where("_id").is(httpRequest.getId()));

        // 3. 创建更新对象（只更新非空字段）
        Update update = new Update();

        // 检查并添加需要更新的字段
        if (httpRequest.getUrl() != null) {
            update.set("url", httpRequest.getUrl());
        }

        if (httpRequest.getMethod() != null) {
            update.set("method", httpRequest.getMethod());
        }

        if (httpRequest.getHeaders() != null && !httpRequest.getHeaders().isEmpty()) {
            update.set("headers", httpRequest.getHeaders());
        }

        if (httpRequest.getBody() != null) {
            update.set("body", httpRequest.getBody());
        }

        // 设置更新时间戳
        update.set("updateTime", new Date());

        // 4. 执行更新操作
        UpdateResult result = mongoTemplate.updateFirst(
                query,
                update,
                HttpRequest.class
        );
        return result.getModifiedCount()>0;
    }

    @Override
    public List<HttpRequest> queryAll(String uid) {
        // 创建查询：匹配 userId 字段
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.find(query, HttpRequest.class);
    }

    @Override
    public Boolean deleteById(String uid, String id) {
        // 组合条件：同时匹配文档ID和用户ID
        Query query = new Query(Criteria
                .where("_id").is(id)
                .and("uid").is(uid)
        );

        DeleteResult result = mongoTemplate.remove(query, HttpRequest.class);
        return result.getDeletedCount() > 0; // 返回是否成功删除
    }

    @Override
    public HttpRequest queryById(String id) {
        // 根据主键直接查询
        return mongoTemplate.findById(id, HttpRequest.class);
    }
}
