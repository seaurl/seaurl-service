package com.seaurl.platform.users.service.impl;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.common.model.UrlCategory;
import com.seaurl.platform.common.model.UserLikeUrl;
import com.seaurl.platform.users.model.UserPoints;
import com.seaurl.platform.users.model.UserPointsHistory;
import com.seaurl.platform.users.service.UserPointsService;
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.count;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;

@Service
public class UserPointsServiceImpl implements UserPointsService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    // 添加积分
    @Override
    public void add(UserPoints userPoints) {
        mongoTemplate.insert(userPoints);
    }

    // 更新积分
    @Override
    public boolean update(UserPoints userPoints) {
        Query query = new Query(Criteria.where("uid").is(userPoints.getUid()));
        Update update = new Update();
        update.inc("points", userPoints.getPoints());
        update.set("updatedDt", new Date());
        long count = mongoTemplate.updateFirst(query, update, UserPoints.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public boolean delete(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        DeleteResult result = mongoTemplate.remove(query, UserPoints.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public UserPoints queryBy(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.findOne(query, UserPoints.class);
    }

    @Override
    public Integer queryPointBy(String uid, String date, String format) {
        log.info("uid={}", uid);
        log.info("date={}", date);
        log.info("format={}", format);
        String dateFormat = "";

        if (format.equals("y")) {
            dateFormat = "%Y";
        }
        if (format.equals("y-m")) {
            dateFormat = "%Y-%m";
        }
        if (format.equals("y-m-d")) {
            dateFormat = "%Y-%m-%d";
        }
        ProjectionOperation project = Aggregation.project("uid", "changePoints")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '" + dateFormat + "', timezone: '+08:00'}}").as("date");

        // 注意：放到group中的字段，如下面的uid，在返回结果的时候，不是本身的数据，所以如果你想在match中匹配会出现问题，
        // 解决办法就是：重新加一个uid,如下所示，或者新增一个project().and("uid").previousOperation())
        GroupOperation group = Aggregation.group("uid", "date").sum("changePoints").as("totalPoints")
                .first("uid").as("uid")
                .first("date").as("date");

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid).and("date").is(date).and("totalPoints").gt(0));

        Aggregation agg = Aggregation.newAggregation(project, group, match);

        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, UserPointsHistory.class, HashMap.class);

        List<HashMap> dataList = outputType.getMappedResults();
        if (dataList.size() > 0) {
            Integer totalPoints = 0;
            for (HashMap<String, Object> map : dataList) {
                totalPoints = (Integer) map.get("totalPoints");
            }
            return totalPoints;
        }
        return 0;
    }
}
