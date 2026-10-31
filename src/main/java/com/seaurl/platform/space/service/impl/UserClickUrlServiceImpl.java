package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.space.model.UserClickUrl;
import com.seaurl.platform.space.service.UserClickUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserClickUrlServiceImpl implements UserClickUrlService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean add(UserClickUrl userClickUrl) {
        mongoTemplate.insert(userClickUrl);
        return true;
    }

    @Override
    public boolean updateClickCount(String id, int count) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();
        update.set("clickCount", count);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, UserClickUrl.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public UserClickUrl queryBy(String uid, String urlId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("urlId").is(urlId));
        return mongoTemplate.findOne(query, UserClickUrl.class);
    }


}
