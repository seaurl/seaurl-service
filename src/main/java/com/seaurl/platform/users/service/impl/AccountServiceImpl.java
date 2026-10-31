package com.seaurl.platform.users.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.model.UserAuth;
import com.seaurl.platform.users.model.UserPointsHistory;
import com.seaurl.platform.users.request.AccountNotice;
import com.seaurl.platform.users.request.AccountRequest;
import com.seaurl.platform.users.request.UserRequest;
import com.seaurl.platform.users.service.AccountService;
import com.seaurl.platform.users.service.aliyun.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {
    private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    public final static String collectionName = "user";

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private MongoOperations mongoOps;

    @Autowired
    OssService ossService;


    @Override
    public boolean isExistUsername(String username) {
        Query query = new Query(Criteria.where("userName").is(username));
        User user = mongoTemplate.findOne(query, User.class);
        return user != null;
    }

    @Override
    public boolean isExistEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        User user = mongoTemplate.findOne(query, User.class);
        return user != null;
    }

    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        Query query = Query.query(Criteria.where("userName").is(userName));
        Update update = Update.update("avatar", avatar);
        long count = mongoTemplate.updateFirst(query, update, User.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public List<User> findAll() {
        List<User> users = mongoTemplate.find(new Query(new Criteria()), User.class);
        return users;
    }



    @Override
    public User getUserByName(String username) {
        Query query = new Query(Criteria.where("userName").is(username));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public Mono<User> getUserByNameMono(String username) {
        Query query = new Query(Criteria.where("userName").is(username));
        return reactiveMongoTemplate.findOne(query, User.class);
    }

    @Override
    public User getUserById(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public Mono<User> getUserByIdMono(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return reactiveMongoTemplate.findOne(query, User.class);
    }

    @Override
    public User getUserByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public void updateUserInfo(UserRequest userRequest) throws Exception {
        Query query = new Query(Criteria.where("userName").is(userRequest.getUserName()));
        Update update = new Update();
        update.set("nickName", userRequest.getNickName());
        update.set("school", userRequest.getSchool());
        update.set("company", userRequest.getCompany());
        update.set("website", userRequest.getWebsite());
        update.set("location", userRequest.getLocation());
        update.set("signature", userRequest.getSignature());
        update.set("introduction", userRequest.getIntroduction());
//        update.set("backgroundImg",userRequest.getBackgroundImg());
        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);
        log.info("更新了{}条数据", result.getModifiedCount());
    }

    @Override
    public void updateUserIpAddr(String uid, String region) {
        Query query = new Query(Criteria.where("uid").is(uid));
        Update update = new Update();
        update.set("ipAddr", region);
        UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);
        log.info("更新了{}条数据", result.getModifiedCount());
    }

    @Override
    public String saveAvatar(String base64Img, String folder) throws IOException {
        return ossService.upload(base64Img, folder);
    }

    @Override
    public boolean updateUserName(String userName, String newUserName) {
        Query query = Query.query(Criteria.where("userName").is(userName));
        Update update = Update.update("userName", newUserName);
        long count = mongoTemplate.updateFirst(query, update, User.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public boolean updateUserNotice(AccountNotice accountNotice) {
        Query query = Query.query(Criteria.where("userName").is(accountNotice.getUserName()));
        Update update = Update.update("notices", accountNotice.getNotices());
        long count = mongoTemplate.updateFirst(query, update, User.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public boolean deleteAccount(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        DeleteResult result1 = mongoTemplate.remove(query, User.class);
        DeleteResult result2 = mongoTemplate.remove(query, UserAuth.class);
        return result1.getDeletedCount() > 0 && result2.getDeletedCount() > 0;
    }

    @Override
    public boolean updateAppearance(String uid, String color, String background) {
        Query query = Query.query(Criteria.where("uid").is(uid));
        Update update = new Update();
        if (StringUtils.hasLength(color)){
            update.set("color", color);
        }
        if (StringUtils.hasLength(background)){
            update.set("background", background);
        }
        long count = mongoTemplate.updateFirst(query, update, User.class).getModifiedCount();
        return count > 0;
    }
}
