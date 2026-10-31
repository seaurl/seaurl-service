package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.service.ChatAiCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ChatAiCategoryServiceImpl implements ChatAiCategoryService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean add(ChatAiCategory chatAiCategory) {
        mongoTemplate.save(chatAiCategory);
        return true;
    }

    @Override
    public ChatAiCategory add2(ChatAiCategory chatAiCategory) {
        return mongoTemplate.save(chatAiCategory);
    }

    @Override
    public boolean edit(ChatAiCategory chatAiCategory) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(chatAiCategory.getId()).and("uid").is(chatAiCategory.getUid()));
        Update update = new Update();
        update.set("title", chatAiCategory.getTitle());
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ChatAiCategory.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean setTop(ChatAiCategory chatAiCategory) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(chatAiCategory.getId()).and("uid").is(chatAiCategory.getUid()));
        Update update = new Update();
        update.set("isTop", chatAiCategory.getIsTop());
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ChatAiCategory.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean remove(String uid, String chatAiId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("chatAiId").is(chatAiId));
        DeleteResult result = mongoTemplate.remove(query, ChatAiCategory.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public List<ChatAiCategory> queryByUid(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        query.with(Sort.by(Sort.Order.desc("isTop"), Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, ChatAiCategory.class);
    }
}
