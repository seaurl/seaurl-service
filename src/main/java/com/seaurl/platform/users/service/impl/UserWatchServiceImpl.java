package com.seaurl.platform.users.service.impl;

import java.util.Arrays;
import com.seaurl.platform.users.controller.UserFollowController;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.common.model.UserWatch;
import com.seaurl.platform.users.service.UserWatchService;
import com.seaurl.platform.users.utils.DateUtils;
import com.seaurl.platform.users.utils.XDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class UserWatchServiceImpl implements UserWatchService {
    private static Logger log = LoggerFactory.getLogger(UserWatchService.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public void add(UserWatch userWatch) {

        Query query = new Query(Criteria.where("uid").is(userWatch.getUid()).and("watchUid").is(userWatch.getWatchUid()));
        // 按时间倒序，获取最新一条记录的数据
        query.with(Sort.by(Sort.Order.desc("createdDt")));
        UserWatch userWatch1 = mongoTemplate.findOne(query, UserWatch.class);
        if (userWatch1 == null) {
            mongoTemplate.insert(userWatch);
        }
        // 注意：如果看了相同的人，每小时记录一次
        else {
            try {
                if (!XDateUtils.isNow(userWatch1.getCreatedDt())) {
                    mongoTemplate.insert(userWatch);
                }
            } catch (Exception e) {
                log.info("exception ={}", e.getMessage());
            }

        }
    }

    @Override
    public List<HashMap> getWatchUserToTodayList(String uid) {
        log.info("uid={}", uid);
        List<AggregationOperation> operationList = Arrays.asList();
        LookupOperation lookupOperation = Aggregation.lookup("user", "uid", "uid", "user_as");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        String current = format.format(now);
        log.info("current={}", current);
        Date start = DateUtils.dateToISODate(current + " 00:00:00");
        Date end = DateUtils.dateToISODate(current + " 23:59:59");
        log.info("start={}", start);
        log.info("end={}", end);

        // 这里的watchUid表示 当前这个人被看了，也就是说是看了'他'
        MatchOperation match = Aggregation.match(Criteria.where("watchUid").is(uid).and("user_as.status").is(2)
                // 查询今天的数据
                .andOperator(Criteria.where("createdDt").gt(start), Criteria.where("createdDt").lt(end))
        );
        ProjectionOperation project = Aggregation.project("user_as.uid", "user_as.userName", "user_as.nickName", "user_as.signature", "user_as.introduction",
                "user_as.email", "user_as.avatar", "user_as.company", "user_as.school", "user_as.location").andInclude("createdDt")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d', timezone: '+08:00'}}").as("date")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%H:%M', timezone: '+08:00'}}").as("time");
//                .dateAsFormattedString("%Y-%m-%d %H:%M:%S ").as("datetime");;

        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        UnwindOperation unwindOperation = Aggregation.unwind("user_as");
        operationList.add(lookupOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(unwindOperation);
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);
//        operationList.add(facet);
        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userWatch", HashMap.class);
        return aggregationResults.getMappedResults();
    }
}
