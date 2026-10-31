package com.seaurl.platform.users.service.impl;

import java.util.Arrays;
import com.seaurl.platform.users.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.users.service.LevelService;
import com.seaurl.platform.users.utils.DateUtils;
import com.seaurl.platform.users.utils.DatesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class LevelServiceImpl implements LevelService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public Level getLevelByPoints(int points) {
        Query query = new Query(Criteria.where("max").lte(points).and("min").gte(points));

        return mongoTemplate.findOne(query, Level.class);
    }

    @Override
    public Level getLevelByUid(String uid) {

        Query query1 = Query.query(Criteria.where("uid").is(uid));
        UserPoints userPoints = mongoTemplate.findOne(query1, UserPoints.class);
        Query query2;
        if (userPoints!=null){
            log.info("userPoints=pppppppppppp{}", userPoints.getPoints());
            query2 = new Query(Criteria.where("maxPoints").gte(userPoints.getPoints()).and("minPoints").lte(userPoints.getPoints()));
        }else{
            query2 = new Query(Criteria.where("maxPoints").lte(1));
        }
        return mongoTemplate.findOne(query2, Level.class);
    }

    /**
     * 查询用户排名
     * @param param
     * @return
     */
    @Override
    public List<Map> getRankListBy(String param) {
        MatchOperation match = null;
        // 今日排行
        if (param.equals("today")){
            Date todayStart = DatesUtil.getDayBegin();
            Date todayEnd = DatesUtil.getDayEnd();
            log.info("todayStart={}", todayStart);
            log.info("todayEnd={}", todayEnd);
            match = Aggregation.match(Criteria.where("createdDt").gte(todayStart).lte(todayEnd));
        }
        // 本周排行
        if (param.equals("week")){
            Date weekStart = DatesUtil.getBeginDayOfWeek();
            Date weekEnd = DatesUtil.getEndDayOfWeek();
            log.info("weekStart={}", weekStart);
            log.info("weekEnd={}", weekEnd);
            match = Aggregation.match(Criteria.where("createdDt").gte(weekStart).lte(weekEnd));
        }
        // 本月排行
        if (param.equals("month")){
            Date monthStart = DatesUtil.getBeginDayOfMonth();
            Date monthEnd = DatesUtil.getEndDayOfMonth();
            log.info("monthStart={}", monthStart);
            log.info("monthEnd={}", monthEnd);
            match = Aggregation.match(Criteria.where("createdDt").gte(monthStart).lte(monthEnd));
        }
        // 本年排行
        if (param.equals("year")){
            Date yearStart = DatesUtil.getBeginDayOfYear();
            Date yearEnd = DatesUtil.getEndDayOfYear();
            log.info("yearStart={}", yearStart);
            log.info("yearEnd={}", yearEnd);
            match = Aggregation.match(Criteria.where("createdDt").gte(yearStart).lte(yearEnd));
        }

        LookupOperation lookup = Aggregation.lookup("user", "uid", "uid", "user_as");
        // 注意：unwind一般情况下要放到紧接LookupOperation后面
        UnwindOperation unwind = Aggregation.unwind("user_as");
        ProjectionOperation project = Aggregation.project("uid", "createdDt",  "user_as.avatar", "user_as.userName", "changePoints").and("user_as.createdDt").as("singUpTime");

        // 注意：在进行sum聚合操作的时候，sum中的字段最好是主表字段，因为关联表的话，会存在数组聚合问题，除非使用unwind才行
        GroupOperation group = Aggregation.group("uid").sum("changePoints").as("totalPoints")
                .first("avatar").as("avatar")
                .first("userName").as("userName")
                .first("singUpTime").as("singUpTime");
        ProjectionOperation project2 = Aggregation.project("totalPoints", "singUpTime", "avatar", "userName").and("uid").previousOperation();
        // 并且取大于0的数据
        MatchOperation match2 = Aggregation.match(Criteria.where("totalPoints").gt(0));

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "totalPoints");
        LimitOperation limit = Aggregation.limit(50);
        List<AggregationOperation> operationList = Arrays.asList();
        operationList.add(lookup);
        operationList.add(unwind);
        operationList.add(project);
        operationList.add(match);
        operationList.add(group);
        operationList.add(match2);
        operationList.add(project2);
        operationList.add(sort);
        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);

        AggregationResults<Map> aggregationResults = mongoTemplate.aggregate(agg, UserPointsHistory.class, Map.class);
        // 6、返回关联查询结果
        return aggregationResults.getMappedResults();
    }

    @Override
    public List<Map> getAllRankList() {
        // 2、按参数顺序：被关联表user，主表apple.uid，被关联表user.id，别名
        LookupOperation lookup = Aggregation.lookup("userPoints", "uid", "uid", "userPoints_as");

        ProjectionOperation project = Aggregation.project("uid", "avatar", "userName")
                .and("createdDt").as("singUpTime")
                .and("userPoints_as.points").as("totalPoints");

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "totalPoints");
        LimitOperation limit = Aggregation.limit(50);
        // 并且取大于0的数据
        MatchOperation match2 = Aggregation.match(Criteria.where("totalPoints").gt(0));

        UnwindOperation unwind = Aggregation.unwind("userPoints_as");
        // 5、添加取List中
        List<AggregationOperation> operationList = Arrays.asList();
        operationList.add(lookup);
        operationList.add(unwind);
        operationList.add(project);
        operationList.add(match2);
        operationList.add(sort);
        operationList.add(limit);

        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<Map> aggregationResults = mongoTemplate.aggregate(agg, User.class, Map.class);
        // 6、返回关联查询结果
        return aggregationResults.getMappedResults();
    }
}
