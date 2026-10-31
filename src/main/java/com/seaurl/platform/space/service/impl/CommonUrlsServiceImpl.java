package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.CommonUrls;
import com.seaurl.platform.space.service.CommonUrlsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommonUrlsServiceImpl implements CommonUrlsService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<CommonUrls> getAll() {
        return mongoTemplate.findAll(CommonUrls.class);
    }

}
