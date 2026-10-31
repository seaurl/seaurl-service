package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.ImageAi;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.space.model.UserClickUrl;
import com.seaurl.platform.space.model.WeatherCity;
import com.seaurl.platform.space.service.WeatherCityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
@Service
public class WeatherCityServiceImpl implements WeatherCityService{

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public Boolean add(WeatherCity weatherCity) {
        mongoTemplate.insert(weatherCity);
        return true;
    }

    @Override
    public Boolean top(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, WeatherCity.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public List<WeatherCity> queryAll(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, WeatherCity.class);
    }

    @Override
    public Boolean delete(String uid, String id) {
//        Query query = new Query(Criteria.where("id").is(id).and("uid").is(uid));
//        Update update = new Update();
//        update.set("isDelete", true);
//        update.set("updatedDt", new Date());
//        UpdateResult result = mongoTemplate.updateFirst(query, update, WeatherCity.class);
//
//        try {
//            // --- 添加提交网址结束 ---
//            return result.getModifiedCount() > 0;
//        } catch (Exception e) {
//            log.info("删除出错：{}", e.getMessage());
//            return false;
//        }
        Query query = new Query(Criteria.where("id").is(id).and("uid").is(uid));
        DeleteResult result = mongoTemplate.remove(query, WeatherCity.class);
        return result.getDeletedCount() > 0;

    }

    @Override
    public Boolean isExit(String uid, String id) {
        Query query = new Query(Criteria.where("uid").is(uid).and("id").is(id));
        List<WeatherCity> result = mongoTemplate.find(query, WeatherCity.class);
        return !result.isEmpty();
    }
}
