package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.service.UserService;
import com.seaurl.platform.space.utils.DateUtils;
import com.seaurl.platform.space.utils.DatesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public User getSpaceUserInfo(String userName) {
        Query query = new Query(Criteria.where("userName").is(userName));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public Map<String, Object> getStatisticalData(String uid) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String lastWeekStartDate = format.format(DatesUtil.getBeginDayOfLastWeek());
        String lastWeekEndDate = format.format(DatesUtil.getEndDayOfLastWeek());

        String currentWeekStartDate = format.format(DatesUtil.getBeginDayOfWeek());
        String currentWeekEndDate = format.format(DatesUtil.getEndDayOfWeek());

        Map<String, Object> map = new HashMap<>();
        Long count1 = getUrlsCount(uid);

        Query query5 = Query.query(Criteria.where("uid").is(uid).and("status").is(true));
        Query query6 = Query.query(Criteria.where("followedId").is(uid).and("status").is(true));
        Long count5 = mongoTemplate.count(query5, UserFollow.class);
        Long count6 = mongoTemplate.count(query6, UserFollow.class);


//        Long count2 = getWatchedCount(uid);
//        Long count3 = getCommonUrlsCount(uid);
//        Long count4 = getTopUrlsCount(uid);
//        Long count5 = getFastCopyUrlsCount(uid);
//        Long count6 = getToolsCount(uid);
//        Double count7 = getUrlsGrowthRate(uid, currentWeekStartDate, currentWeekEndDate, lastWeekStartDate, lastWeekEndDate);
//        Double count8 = getWatchedGrowthRate(uid, currentWeekStartDate, currentWeekEndDate, lastWeekStartDate, lastWeekEndDate);

        map.put("urlsCount", count1);
        map.put("followingCount", count5);
        map.put("followerCount", count6);
//        map.put("watchCount", count2);
//        map.put("userCommonUrlsCount", count3);
//        map.put("topUrlsCount", count4);
//        map.put("fastCopyUrlsCount", count5);
//        map.put("userToolsCount", count6);
//
//        map.put("urlsGrowthRate", count7);
//        map.put("watchedGrowthRate", count8);
        return map;
    }

    //网址增长率数量
    private Double getUrlsGrowthRate(String uid, String currentWeekStartDate, String currentWeekEndDate, String lastWeekStartDate, String lastWeekEndDate) {
        Query query1 = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false).andOperator(
                Criteria.where("createdDt").gte(DateUtils.dateToISODate(lastWeekStartDate)),
                Criteria.where("createdDt").lte(DateUtils.dateToISODate(lastWeekEndDate))));

        Query query2 = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false).andOperator(
                Criteria.where("createdDt").gte(DateUtils.dateToISODate(currentWeekStartDate)),
                Criteria.where("createdDt").lte(DateUtils.dateToISODate(currentWeekEndDate))));

        long preWeekCount = mongoTemplate.count(query1, Url.class);
        long currentWeekCount = mongoTemplate.count(query2, Url.class);
        //如果上期为0，则增长率为100%
        if (preWeekCount == 0 && currentWeekCount != 0) {
            return 1.00;
        }
        //如果上期为0，本期也为0，则返回0
        if(preWeekCount == 0 && currentWeekCount == 0){
            return 0.00;
        }
        return (currentWeekCount - preWeekCount) * 1.00 / preWeekCount;
    }

    //用户访问增长率数量
    private Double getWatchedGrowthRate(String uid, String currentWeekStartDate, String currentWeekEndDate, String lastWeekStartDate, String lastWeekEndDate) {
        Query query1 = new Query(Criteria.where("watchUid").is(uid).andOperator(
                Criteria.where("createdDt").gte(DateUtils.dateToISODate(lastWeekStartDate)),
                Criteria.where("createdDt").lte(DateUtils.dateToISODate(lastWeekEndDate))));

        Query query2 = new Query(Criteria.where("watchUid").is(uid).andOperator(
                Criteria.where("createdDt").gte(DateUtils.dateToISODate(currentWeekStartDate)),
                Criteria.where("createdDt").lte(DateUtils.dateToISODate(currentWeekEndDate))));

        long preWeekCount = mongoTemplate.count(query1, UserWatch.class);
        long currentWeekCount = mongoTemplate.count(query2, UserWatch.class);
        //如果上期为0，则增长率为100%
        if (preWeekCount == 0 && currentWeekCount != 0) {
            return 1.00;
        }
        //如果上期为0，本期也为0，则返回0
        if(preWeekCount == 0 && currentWeekCount == 0){
            return 0.00;
        }
        return (currentWeekCount - preWeekCount) * 1.00 / preWeekCount;
    }

    // 用户网址数量
    private Long getUrlsCount(String uid) {
        Query query = Query.query(Criteria.where("uid").is(uid).and("isDelete").is(false));
        return mongoTemplate.count(query, Url.class);
    }

    //用户访问数量
    private Long getWatchedCount(String uid) {
        // '我'被别人查看
        Query query = new Query(Criteria.where("watchUid").is(uid));
        return mongoTemplate.count(query, UserWatch.class);
    }

    // 用户常用网址数量
    private Long getCommonUrlsCount(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.count(query, UserCommonUrls.class);
    }

    // 用户关注网址数量
    private Long getTopUrlsCount(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isAttention").is(true));
        return mongoTemplate.count(query, Url.class);
    }

    // 用户快速复制网址数量
    private Long getFastCopyUrlsCount(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isFastCopy").is(true));
        return mongoTemplate.count(query, Url.class);
    }

    // 用户小工具数量
    private Long getToolsCount(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.count(query, UserTools.class);
    }
}
