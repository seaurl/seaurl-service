package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.Hotline;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.space.service.HotlineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotlineServiceImpl implements HotlineService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public Hotline add(Hotline hotline) {
        return mongoTemplate.save(hotline);
    }

    @Override
    public boolean edit(Hotline hotline) {
        return false;
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    @Override
    public List<Hotline> queryAll() {
        List<Hotline> hotlines = mongoTemplate.find(new Query(new Criteria()), Hotline.class);
        return hotlines;
    }
}
