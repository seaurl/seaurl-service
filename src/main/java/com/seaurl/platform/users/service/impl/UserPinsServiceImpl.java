package com.seaurl.platform.users.service.impl;

import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.model.UserPins;
import com.seaurl.platform.users.service.UserPinsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserPinsServiceImpl implements UserPinsService {

    @Autowired
    MongoTemplate mongoTemplate;


    @Override
    public UserPins add(UserPins userPins) {
        return mongoTemplate.insert(userPins);
    }

    @Override
    public boolean update(UserPins userPins) {
        Query query = Query.query(Criteria.where("uid").is(userPins.getUid()));
        Update update = Update.update("urlList", userPins.getUrlList());
        long count = mongoTemplate.updateFirst(query, update, UserPins.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public UserPins queryByUid(String uid) {
        Query query = Query.query(Criteria.where("uid").is(uid));
        UserPins userPins = mongoTemplate.findOne(query, UserPins.class);
        return userPins;
    }
}
