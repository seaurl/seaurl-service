package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.CommonUrls;
import com.seaurl.platform.space.model.CommonUrlsGroup;
import com.seaurl.platform.space.service.CommonUrlsGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommonUrlsGroupServiceImpl implements CommonUrlsGroupService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<CommonUrlsGroup> getAll() {
        return mongoTemplate.findAll(CommonUrlsGroup.class);
    }
}
