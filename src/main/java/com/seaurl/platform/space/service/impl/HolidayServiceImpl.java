package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.Holiday;
import com.seaurl.platform.space.service.HolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HolidayServiceImpl implements HolidayService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<Holiday> queryBy(String year) {
        // 构建查询条件：date 字段以指定年份开头
        Criteria criteria = Criteria.where("date").regex("^" + year + "-");
        Query query = new Query(criteria);

        // 执行查询
        return mongoTemplate.find(query, Holiday.class);
    }
}
