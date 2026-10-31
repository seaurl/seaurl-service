package com.seaurl.platform.users.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.UserFollow;
import com.seaurl.platform.users.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("usersUserFollowServiceImpl")
public class UserFollowServiceImpl implements UserFollowService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean follwedUser(UserFollow userFollow) {
        // 1、判断用户有没有关注
        Query query = new Query(Criteria.where("uid").is(userFollow.getUid()).and("followedId").is(userFollow.getFollowedId()));
        UserFollow queryUserFollow = mongoTemplate.findOne(query, UserFollow.class);
        if (queryUserFollow != null) {
            Update update = new Update();
            update.set("status", userFollow.getStatus());
            update.set("updatedDt", new Date());
            UpdateResult result = mongoTemplate.updateFirst(query, update, UserFollow.class);
            return result.getModifiedCount() > 0;
        } else {
            mongoTemplate.insert(userFollow);
            return true;
        }
    }

    @Override
    public boolean isFollowed(String uid, String followedUserId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("followedId").is(followedUserId));
        UserFollow queryUserFollow = mongoTemplate.findOne(query, UserFollow.class);
        if (queryUserFollow != null) {
            return queryUserFollow.getStatus() != null && queryUserFollow.getStatus() == 1;
        } else {
            return false;
        }
    }

    @Override
    public List<UserFollow> getMyFollowUserList(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("status").is(true));

        return mongoTemplate.find(query, UserFollow.class);
    }
}
