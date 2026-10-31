package com.seaurl.platform.users.service.impl;

import com.seaurl.platform.common.model.User;
import com.seaurl.platform.common.model.UserLikeUrl;
import com.seaurl.platform.users.service.UserLikeUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserLikeUrlServiceImpl implements UserLikeUrlService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;


    /**
     * 更新点赞数据
     * @param userLikeUrl
     * @return
     */
    @Override
    public boolean update(UserLikeUrl userLikeUrl) {
        log.info("count---{}",userLikeUrl.getValid());
        Query query = new Query(Criteria.where("uid").is(userLikeUrl.getUid()).and("urlId").is(userLikeUrl.getUrlId()));
        Update update = new Update();
        update.set("valid", userLikeUrl.getValid());
        update.set("updatedDt", userLikeUrl.getUpdatedDt());
        long count = mongoTemplate.updateFirst(query, update, UserLikeUrl.class).getModifiedCount();
        return count > 0;
    }

    /**
     * 添加点赞数据
     * @param userLikeUrl
     */
    @Override
    public void add(UserLikeUrl userLikeUrl) {
        mongoTemplate.insert(userLikeUrl);
    }

    /**
     * 是否存在已点赞的数据
     * @param uid
     * @param urlId
     * @return
     */
    @Override
    public UserLikeUrl isExist(String uid, String urlId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("urlId").is(urlId));
        return mongoTemplate.findOne(query, UserLikeUrl.class);
    }

    /**
     *  根据uid+valid=1+urlid结合来判断用户是否点赞了，并返回列表
     * @param uid
     * @param urlId
     * @return
     */
    @Override
    public List<UserLikeUrl> queryLikeUrlByUserId(String uid, List<String> urlId) {
        log.info("userids={}",urlId);
        Query query = new Query(Criteria.where("uid").is(uid).and("valid").is(1).and("urlId").in(urlId));
        return mongoTemplate.find(query, UserLikeUrl.class);
    }
}
