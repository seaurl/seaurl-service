package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.service.UserAiToolsService;
import org.bson.Document;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class UserAiToolsServiceImpl implements UserAiToolsService {

    private static Logger log = LoggerFactory.getLogger(UserAiToolsServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public UserAiTools add(UserAiTools userAiTools) {
        return mongoTemplate.insert(userAiTools);
    }

    @Override
    public boolean delete(String usedId) {
        Query query = new Query(Criteria.where("usedId").is(usedId));
        DeleteResult result = mongoTemplate.remove(query, UserAiTools.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean update(UserAiTools userAiTools) {
        return false;
    }

    @Override
    public boolean updateDt(String usedId) {
        Date date = new Date();
        Query query = new Query(Criteria.where("usedId").is(usedId));
        Update update = new Update();
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, UserAiTools.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean setTop(UserAiTools userAiTools) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(userAiTools.getId()));
        Update update = new Update();
        update.set("isTop", userAiTools.getIsTop());
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, UserAiTools.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public List<UserAiTools> query(UserAiTools userAiTools) {
        return null;
    }

    @Override
    public List<HashMap> queryBy(String uid) {
        log.info("--------uid={}", uid);
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();

        LookupOperation lookupOperation = Aggregation.lookup("userAiTools", "_id", "aiToolId", "userAiTools");

        MatchOperation matchOperation = Aggregation.match(Criteria.where("show").is(true).and("userAiTools.uid").is(uid));

        ProjectionOperation project = Aggregation.project("name", "desc", "type", "model", "avatar", "command", "sort", "createdDt", "updatedDt")
                .and("userAiTools.usedId").as("usedId")
                .and("userAiTools.uid").as("uid")
                .and("userAiTools.updatedDt").as("updatedDt2");
        UnwindOperation unwindOperation = Aggregation.unwind("userAiTools");
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt2");

        List<AggregationOperation> operationList = new ArrayList<>();
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
        operationList.add(matchOperation);
        operationList.add(project);
        operationList.add(sort);

        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, AiTools.class, HashMap.class);
        return aggregationResults.getMappedResults();
    }

    @Override
    public List<UserAiTools> queryByUsedId(String usedId) {
        Query query = new Query(Criteria.where("uid").is(usedId));
        return mongoTemplate.find(query, UserAiTools.class);
    }

    @Override
    public List<UserAiTools> queryBy(String uid, String aiToolId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("aiToolId").is(aiToolId));
        return mongoTemplate.find(query, UserAiTools.class);
    }
}
