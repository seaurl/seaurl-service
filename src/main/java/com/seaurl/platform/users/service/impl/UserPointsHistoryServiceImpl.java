package com.seaurl.platform.users.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.users.model.UserPoints;
import com.seaurl.platform.users.model.UserPointsHistory;
import com.seaurl.platform.users.service.UserPointsHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class UserPointsHistoryServiceImpl implements UserPointsHistoryService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;


    @Override
    public void add(UserPointsHistory userPointsHistory) {
        mongoTemplate.insert(userPointsHistory);
    }

    @Override
    public boolean update(UserPointsHistory userPointsHistory) {
        return false;
    }

    @Override
    public boolean delete(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        DeleteResult result = mongoTemplate.remove(query, UserPointsHistory.class);
        return result.getDeletedCount() > 0;
    }
}
