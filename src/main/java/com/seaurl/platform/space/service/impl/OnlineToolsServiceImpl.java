package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.space.model.OnlineTools;
import com.seaurl.platform.space.model.OnlineToolsGroup;
import com.seaurl.platform.space.model.UserSoftware;
import com.seaurl.platform.space.model.UserTools;
import com.seaurl.platform.space.service.OnlineToolsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class OnlineToolsServiceImpl implements OnlineToolsService {

    @Autowired
    MongoTemplate mongoTemplate;


    @Override
    public boolean add(OnlineTools onlineTools) {
        mongoTemplate.insert(onlineTools);
        return true;
    }

    @Override
    public boolean remove(String uid, String toolId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("id").is(toolId));
        DeleteResult result = mongoTemplate.remove(query, OnlineTools.class);
        return result.getDeletedCount()>0;
    }

    @Override
    public List<OnlineTools> queryById(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        List<UserTools> userToolsList = mongoTemplate.find(query, UserTools.class);
        List<String> toolIds = new ArrayList<>();
        userToolsList.forEach(item -> toolIds.add(item.getToolId()));

        Query query2 = new Query(Criteria.where("id").in(toolIds));

        return mongoTemplate.find(query2, OnlineTools.class);
    }

    @Override
    public UserTools queryBy(String uid, String toolId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("toolId").is(toolId));
        return mongoTemplate.findOne(query, UserTools.class);
    }

    @Override
    public List<OnlineToolsGroup> queryAll() {
        return mongoTemplate.findAll(OnlineToolsGroup.class);
    }

    @Override
    public List<OnlineTools> queryByParam(String toolGroupId, String param) {
        Pattern pattern = Pattern.compile("^.*" + param + ".*$", Pattern.CASE_INSENSITIVE);
        Query query = new Query(Criteria.where("groupId").is(toolGroupId).and("name").regex(pattern));
        return mongoTemplate.find(query, OnlineTools.class);
    }

    @Override
    public List<OnlineTools> queryByParam(String uid, String toolGroupId, String param) {
        Pattern pattern = Pattern.compile("^.*" + param + ".*$", Pattern.CASE_INSENSITIVE);
        Query query = new Query(Criteria.where("groupId").is(toolGroupId).and("name").regex(pattern).and("uid").is(uid));
        return mongoTemplate.find(query, OnlineTools.class);
    }
}
