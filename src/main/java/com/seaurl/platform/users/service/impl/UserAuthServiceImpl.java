package com.seaurl.platform.users.service.impl;

import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.model.UserAuth;
import com.seaurl.platform.users.service.UserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    @Autowired
    MongoTemplate mongoTemplate;

    private final String USER_AUTH = "userAuth";

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    /**
     * 修改用户密码
     * @param username
     * @param email
     * @param pwd
     * @return
     */
    @Override
    public boolean updateUserPassword(String username, String email, String pwd) {
        String newpwd = bCryptPasswordEncoder.encode(pwd);
        Criteria criteria = new Criteria();
        criteria.orOperator(Criteria.where("userName").is(username), Criteria.where("email").is(email));
        Update update = Update.update("password", newpwd);
        long count = mongoTemplate.updateMulti(new Query(criteria), update, UserAuth.class, USER_AUTH).getModifiedCount();
        return count > 0;
    }

    /**
     * 更新账户密码
     * @param userName
     * @param newPwd
     * @return
     */
    @Override
    public boolean updateUserPassword(String userName, String newPwd) {
        Query query = new Query(Criteria.where("userName").is(userName));
        Update update = Update.update("password", newPwd);
        long count = mongoTemplate.updateFirst(query, update, UserAuth.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public boolean updateUserName(String userName, String newUserName) {
        Query query = Query.query(Criteria.where("userName").is(userName));
        Update update = Update.update("userName", newUserName);
        long count = mongoTemplate.updateFirst(query, update, UserAuth.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public boolean updateUserEmail(String userName, String newEmail) {
        Query query = Query.query(Criteria.where("userName").is(userName));
        Update update = Update.update("email", newEmail);
        long count1 = mongoTemplate.updateFirst(query, update, UserAuth.class).getModifiedCount();
        long count2 = mongoTemplate.updateFirst(query, update, User.class).getModifiedCount();
        return count1 > 0 && count2 > 0;
    }

    @Override
    public boolean isVerified(String username) {
        Query query = new Query(Criteria.where("userName").is(username));
        UserAuth user = mongoTemplate.findOne(query, UserAuth.class);
        return user.isVerified();
    }

    @Override
    public boolean updateUserVerified(String username) {
        Query query = Query.query(Criteria.where("userName").is(username));
        Update update = Update.update("isVerified", true);
        long count = mongoTemplate.updateFirst(query, update, UserAuth.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public UserAuth getUserByName(String username) {
        Criteria criteria = new Criteria();
        criteria.orOperator(
                Criteria.where("userName").is(username),
                Criteria.where("email").is(username)
        );
        Query query = new Query(criteria);
        return mongoTemplate.findOne(query, UserAuth.class);
    }

    @Override
    public User getUserByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.findOne(query, User.class);
    }
}
