package com.seaurl.platform.users.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.users.model.UserDevice;
import com.seaurl.platform.common.model.UserEvent;
import com.seaurl.platform.common.model.UserFollow;
import com.seaurl.platform.users.service.UserDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDeviceServiceImpl implements UserDeviceService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public void add(com.seaurl.platform.users.model.UserDevice userDevice) {
        mongoTemplate.insert(userDevice);
    }

    @Override
    public boolean deleteAll(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        DeleteResult result = mongoTemplate.remove(query, UserDevice.class);

        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean deleteById(String tokenId) {
        Query query = new Query(Criteria.where("tokenId").is(tokenId));
        DeleteResult result = mongoTemplate.remove(query, UserDevice.class);

        return result.getDeletedCount() > 0;
    }

    @Override
    public List<UserDevice> queryByUid(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));

        return mongoTemplate.find(query, UserDevice.class);
    }
}
