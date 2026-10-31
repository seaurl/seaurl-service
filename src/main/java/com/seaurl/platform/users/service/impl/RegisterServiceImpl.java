package com.seaurl.platform.users.service.impl;


import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.common.config.ServiceConfig;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.model.UserAuth;
import com.seaurl.platform.users.request.RegisterRequest;
import com.seaurl.platform.users.service.RegisterService;
import com.seaurl.platform.users.service.aliyun.OssService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Stack;

@Service
public class RegisterServiceImpl implements RegisterService {
    private static Logger log = LoggerFactory.getLogger(RegisterServiceImpl.class);


    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    OssService ossService;

    @Autowired
    ServiceConfig serviceConfig;

    @Autowired
    AliyunConfig aliyunOssConfig;

    /**
     * 注册
     *
     * @param registerRequest
     * @return
     */
    @Override
    public String register(RegisterRequest registerRequest) {
        try {
            String env = serviceConfig.getServerEnv();
            String avatar = "users/avatar/" + registerRequest.getUsername() + "/" + env + "/" + System.currentTimeMillis();

//            String avatar = "users/avatar/" + registerRequest.getUsername() + "/" + System.currentTimeMillis();
            // 每次用户第一次注册的时候就把默认图片复制给注册的新用户
            ossService.copyTo("users/default/default.png", avatar);

            Date current = new Date();
            UserAuth userAuth = new UserAuth();
            User user = new User();

            ObjectId uid = new ObjectId();
            user.setUid(uid.toString());
            user.setUserName(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setEmailBindTime(new Date().getTime());
            user.setStatus(2);
            user.setType(1);
            user.setAvatar(aliyunOssConfig.getOssBucketCdnHost() + "/" + avatar);
            user.setIpAddr(registerRequest.getRegion());
            user.setNotices(Arrays.asList("copyMyUrlNotice","shareFileToMe", "followingMeNotice", "userSendMessageToMeNotice", "systemSendMessageToUserNotice"));
            user.setCreatedDt(current);
            user.setUpdatedDt(current);
            mongoTemplate.insert(user);

            userAuth.setUid(uid.toString());
            userAuth.setUserName(registerRequest.getUsername());
            userAuth.setEmail(registerRequest.getEmail());
            userAuth.setPassword(bCryptPasswordEncoder.encode(registerRequest.getPassword()));
            userAuth.setVerified(false);
            userAuth.setAdmin(false);
            userAuth.setRoles(List.of("ROLE_USER"));
            userAuth.setCreatedDt(current);
            userAuth.setUpdatedDt(current);
            mongoTemplate.insert(userAuth);
            return uid.toString();
        } catch (Exception e) {
            log.info("注册失败：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean hasUsername(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(username));
        UserAuth user = mongoTemplate.findOne(query, UserAuth.class);
        return user != null;
    }

    @Override
    public boolean hasEmail(String email) {
        Query query = new Query();
        query.addCriteria(Criteria.where("email").is(email));
        UserAuth user = mongoTemplate.findOne(query, UserAuth.class);
        return user != null;
    }
}
