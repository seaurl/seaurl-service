package com.seaurl.platform.users.service.impl;


import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.platform.users.model.MailVerified;
import com.seaurl.platform.users.service.MailVerifiedService;
import com.seaurl.platform.users.service.UserAuthService;
import com.seaurl.platform.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class MailVerifiedServiceImpl implements MailVerifiedService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    UserService userService;

    @Autowired
    UserAuthService userAuthService;

    @Override
    public void add(MailVerified mailVerified) {
        mongoTemplate.insert(mailVerified);
    }

    @Override
    public long removeByUsername(String username) {
        Query query = new Query(Criteria.where("username").is(username));
        long count = mongoTemplate.remove(query, MailVerified.class).getDeletedCount();
        return count;
    }

    @Override
    public ResultInfo verified(String username, String code) {
        try {
            Criteria c = Criteria.where("username").is(username).andOperator(Criteria.where("code").is(code));
            Query query = new Query();
            query.addCriteria(c);
            MailVerified mailVerified = mongoTemplate.findOne(query, MailVerified.class);
            if (mailVerified == null){
                return new ResultInfo(ResultStatus.UN_QUERY_DATA);
            }
            Calendar dateOne = Calendar.getInstance();
            Calendar dateTwo = Calendar.getInstance();
            dateOne.setTime(new Date());
            dateTwo.setTime(mailVerified.getCreatedDt());
            long timeOne = dateOne.getTimeInMillis();
            long timeTwo = dateTwo.getTimeInMillis();
            long minute = (timeOne - timeTwo) / (1000 * 60);
            if (minute < 30) {
                long count = removeByUsername(username);
                if (count>0){
                    boolean success = userAuthService.updateUserVerified(username);
                    return success? new ResultInfo("验证邮箱成功"):
                            new ResultInfo(ResultStatus.UPDATE_ERROR);
                }
                return new ResultInfo(ResultStatus.DELETE_ERROR);
            }
            removeByUsername(username);
            return new ResultInfo(ResultStatus.VERIFIED_EXPIRED);
        } catch (Exception e) {
            return new ResultInfo(ResultStatus.EMAIL_FAILURE);
        }
    }
}
