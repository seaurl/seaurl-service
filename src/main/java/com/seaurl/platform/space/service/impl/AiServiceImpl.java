package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.AiConfig;
import com.seaurl.platform.space.model.AiMessage;
import com.seaurl.platform.space.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiServiceImpl implements AiService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public AiConfig getConfig(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.findOne(query, AiConfig.class);
    }

    @Override
    public AiConfig saveConfig(String uid, String model, String apiKey, String baseUrl) {
        Query query = new Query(Criteria.where("uid").is(uid));
        AiConfig existing = mongoTemplate.findOne(query, AiConfig.class);
        Date now = new Date();

        if (existing != null) {
            Update update = new Update();
            if (StringUtils.hasLength(model)) update.set("model", model);
            if (StringUtils.hasLength(apiKey) && !"__UNCHANGED__".equals(apiKey)) {
                update.set("apiKey", apiKey);
            }
            if (baseUrl != null) update.set("baseUrl", baseUrl);
            update.set("updatedDt", now);
            mongoTemplate.updateFirst(query, update, AiConfig.class);
            return mongoTemplate.findOne(query, AiConfig.class);
        }

        if (!StringUtils.hasLength(apiKey) || "__UNCHANGED__".equals(apiKey)) {
            return null;
        }

        AiConfig config = new AiConfig();
        config.setUid(uid);
        config.setModel(model);
        config.setApiKey(apiKey);
        config.setBaseUrl(baseUrl);
        config.setCreatedDt(now);
        config.setUpdatedDt(now);
        return mongoTemplate.insert(config);
    }

    @Override
    public AiMessage saveMessage(String uid, String conversationId, String role, String content, String title) {
        AiMessage msg = new AiMessage();
        msg.setUid(uid);
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTitle(title);
        Date now = new Date();
        msg.setCreatedDt(now);
        msg.setUpdatedDt(now);
        return mongoTemplate.insert(msg);
    }

    @Override
    public List<AiMessage> getMessages(String uid, String conversationId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("conversationId").is(conversationId));
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdDt"));
        return mongoTemplate.find(query, AiMessage.class);
    }

    @Override
    public List<String> getConversations(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdDt"));
        List<AiMessage> all = mongoTemplate.find(query, AiMessage.class);
        Set<String> set = new LinkedHashSet<>();
        for (AiMessage msg : all) {
            if (msg.getConversationId() != null) {
                set.add(msg.getConversationId());
            }
        }
        return new ArrayList<>(set);
    }

    @Override
    public boolean deleteConversation(String uid, String conversationId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("conversationId").is(conversationId));
        var result = mongoTemplate.remove(query, AiMessage.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean deleteAllConversations(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        var result = mongoTemplate.remove(query, AiMessage.class);
        return result.getDeletedCount() > 0;
    }
}
