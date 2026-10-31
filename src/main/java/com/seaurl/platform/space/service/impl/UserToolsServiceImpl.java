package com.seaurl.platform.space.service.impl;

import java.util.Arrays;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.UrlCategory;
import com.seaurl.platform.space.model.UserTools;
import com.seaurl.platform.space.service.UserToolsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class UserToolsServiceImpl implements UserToolsService {
    @Autowired
    MongoTemplate mongoTemplate;


    @Override
    public boolean addAll(List<UserTools> userToolsList) {
        mongoTemplate.insertAll(userToolsList);
        return true;
    }

    @Override
    public boolean add(UserTools userTools) {
        mongoTemplate.insert(userTools);
        return true;
    }

    @Override
    public boolean edit(UserTools userTools) {
        return false;
    }

    @Override
    public boolean remove(String uid, String toolId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("toolId").is(toolId));
        DeleteResult result = mongoTemplate.remove(query, UserTools.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public List<UserTools> queryByUid(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.find(query, UserTools.class);
    }

    @Override
    public List<HashMap> queryUserTools(String uid) {
        List<AggregationOperation> operationList = Arrays.asList();
        AddFieldsOperation add1FieldsOperation = AddFieldsOperation.addField("toolId").withValue(ConvertOperators.ToObjectId.toObjectId("$toolId")).build();

        LookupOperation lookupOperation = Aggregation.lookup("tools", "toolId", "_id", "tools_as");

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid));
        ProjectionOperation project = Aggregation.project("tools_as.name", "tools_as.url", "tools_as.character", "uid", "createdDt")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d %H:%M:%S', timezone: '+08:00'}}").as("date");
// sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        UnwindOperation unwindOperation = Aggregation.unwind("tools_as");
        operationList.add(add1FieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
//因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);

        Aggregation agg = Aggregation.newAggregation(operationList);
// collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userTools", HashMap.class);
        return aggregationResults.getMappedResults();
    }

    @Override
    public UserTools queryBy(String uid, String toolId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("toolId").is(toolId));
        return mongoTemplate.findOne(query, UserTools.class);
    }
}
