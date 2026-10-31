package com.seaurl.platform.space.service.impl;

import java.util.Arrays;
import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.space.model.UserCommonUrls;
import com.seaurl.platform.space.model.UserTools;
import com.seaurl.platform.space.service.UserCommonUrlsService;
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
public class UserCommonUrlsServiceImpl implements UserCommonUrlsService {

    @Autowired
    MongoTemplate mongoTemplate;


    @Override
    public boolean add(UserCommonUrls userCommonUrls) {
        mongoTemplate.insert(userCommonUrls);
        return true;
    }

    @Override
    public boolean edit(UserCommonUrls userCommonUrls) {
        return false;
    }

    @Override
    public boolean remove(String uid, String groupId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("groupId").is(groupId));
        DeleteResult result = mongoTemplate.remove(query, UserCommonUrls.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public List<UserCommonUrls> queryByUid(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.find(query, UserCommonUrls.class);
    }

    @Override
    public List<HashMap> queryUserCommonUrls(String uid) {
        List<AggregationOperation> operationList = Arrays.asList();
        AddFieldsOperation add1FieldsOperation = AddFieldsOperation.addField("groupId").withValue(ConvertOperators.ToObjectId.toObjectId("$groupId")).build();

        LookupOperation lookupOperation = Aggregation.lookup("commonUrlsGroup", "groupId", "_id", "commonUrlsGroup_as");

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid));
        ProjectionOperation project = Aggregation.project("commonUrlsGroup_as.group", "commonUrlsGroup_as.groupName", "commonUrlsGroup_as.commonUrlsList", "uid", "createdDt")
                .andExpression("{ $dateToString: {date: '$createdDt', format: '%Y-%m-%d %H:%M:%S', timezone: '+08:00'}}").as("date");
// sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
        UnwindOperation unwindOperation = Aggregation.unwind("commonUrlsGroup_as");
        operationList.add(add1FieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
//因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);

        Aggregation agg = Aggregation.newAggregation(operationList);
// collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userCommonUrls", HashMap.class);
        return aggregationResults.getMappedResults();
    }

    @Override
    public UserCommonUrls queryBy(String uid, String groupId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("groupId").is(groupId));
        return mongoTemplate.findOne(query, UserCommonUrls.class);
    }
}
