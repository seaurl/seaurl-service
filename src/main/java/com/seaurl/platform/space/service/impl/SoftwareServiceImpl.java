package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.space.model.Software;
import com.seaurl.platform.space.model.SoftwareGroup;
import com.seaurl.platform.space.model.UserSoftware;
import com.seaurl.platform.space.service.SoftwareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


@Service
public class SoftwareServiceImpl implements SoftwareService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean add(UserSoftware userSoftware) {
        mongoTemplate.insert(userSoftware);
        return true;
    }

    @Override
    public boolean remove(String uid, String softwareId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("softwareId").is(softwareId));
        DeleteResult result = mongoTemplate.remove(query, UserSoftware.class);

        return result.getDeletedCount() > 0;
    }

    @Override
    public List<Software> queryById(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));

        List<UserSoftware> userSoftwares = mongoTemplate.find(query, UserSoftware.class);
        List<String> softwareIds = new ArrayList<>();
        userSoftwares.forEach(item -> softwareIds.add(item.getSoftwareId()));
        Query query2 = new Query(Criteria.where("id").in(softwareIds));

        return mongoTemplate.find(query2, Software.class);
    }

    @Override
    public UserSoftware queryBy(String uid, String softwareId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("softwareId").is(softwareId));

        return mongoTemplate.findOne(query, UserSoftware.class);
    }

    @Override
    public List<SoftwareGroup> queryAll() {
        return mongoTemplate.findAll(SoftwareGroup.class);
    }

    @Override
    public List<Software> queryByParam(String groupId, String param) {
        Pattern pattern = Pattern.compile("^.*" + param + ".*$", Pattern.CASE_INSENSITIVE);
        Query query;
        if (StringUtils.hasLength(groupId)){
            query = new Query(Criteria.where("groupId").is(groupId).and("name").regex(pattern));
        }else{
            query = new Query(Criteria.where("name").regex(pattern));
        }
        return mongoTemplate.find(query, Software.class);
    }
}
