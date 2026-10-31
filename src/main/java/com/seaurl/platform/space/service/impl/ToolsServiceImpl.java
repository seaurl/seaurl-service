package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.OnlineTools;
import com.seaurl.platform.space.service.ToolsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolsServiceImpl implements ToolsService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<OnlineTools> getAll() {
        return mongoTemplate.findAll(OnlineTools.class);
    }
}
