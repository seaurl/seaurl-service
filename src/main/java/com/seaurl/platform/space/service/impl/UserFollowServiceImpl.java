package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.common.model.UserFollow;
import com.seaurl.platform.space.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserFollowServiceImpl implements UserFollowService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean follwedUser(UserFollow userFollow) {
        mongoTemplate.insert(userFollow);
        return true;
    }
}
