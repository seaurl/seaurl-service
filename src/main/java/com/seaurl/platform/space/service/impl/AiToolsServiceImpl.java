package com.seaurl.platform.space.service.impl;

import java.util.Arrays;
import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.service.AiToolsService;
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
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class AiToolsServiceImpl implements AiToolsService {

    private static Logger log = LoggerFactory.getLogger(AiToolsServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;
    @Override
    public AiTools add(AiTools aiTools) {
        return mongoTemplate.insert(aiTools);
    }

    @Override
    public boolean delete(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        DeleteResult result = mongoTemplate.remove(query, AiTools.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean delete(String uid, String id) {
        Query query = new Query(Criteria.where("id").is(id).and("uid").is(uid));
        DeleteResult result = mongoTemplate.remove(query, AiTools.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean update(AiTools aiTools) {
        Query query = new Query(Criteria.where("id").is(aiTools.getId()));
        Update update = new Update();
        if (StringUtils.hasLength(aiTools.getName())){
            update.set("name", aiTools.getName());
        }
        if (StringUtils.hasLength(aiTools.getAvatar())){
            update.set("avatar", aiTools.getAvatar());
        }
        if (StringUtils.hasLength(aiTools.getDesc())){
            update.set("desc", aiTools.getDesc());
        }
        if (StringUtils.hasLength(aiTools.getType())){
            update.set("type", aiTools.getType());
        }
        if (StringUtils.hasLength(aiTools.getCommand())){
            update.set("command", aiTools.getCommand());
        }
        if (StringUtils.hasLength(aiTools.getModel())){
            update.set("model", aiTools.getModel());
        }
        if (StringUtils.hasLength(aiTools.getOrigin())){
            update.set("origin", aiTools.getOrigin());
        }
        if (StringUtils.hasLength(aiTools.getBackground())){
            update.set("background", aiTools.getBackground());
        }
        if (StringUtils.hasLength(aiTools.getEndpoint())){
            update.set("endpoint", aiTools.getEndpoint());
        }
        if (StringUtils.hasLength(aiTools.getInvoke())){
            update.set("invoke", aiTools.getInvoke());
        }

        update.set("updatedDt", new Date());

        long count = mongoTemplate.updateFirst(query, update, AiTools.class).getModifiedCount();
        return count > 0;

    }

    @Override
    public List<HashMap> queryAll(String uid) {
//        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();
//
//        LookupOperation lookupOperation = Aggregation.lookup("aiTools", "_id", "groupId", "aiTools");
//
//        // Step 2: Use $addFields with $map to convert _id in aiTools to string
//        // 将数组中的id转化成新字段
//        AddFieldsOperation convertIdsToString = Aggregation.addFields()
//                .addFieldWithValue("aiTools",
//                        new Document("$map",
//                                new Document("input", "$aiTools")
//                                        .append("as", "item")
//                                        .append("in",
//                                                new Document("$mergeObjects", List.of(
//                                                        "$$item",
//                                                        new Document("aiToolId", new Document("$toString", "$$item._id"))
//                                                ))))).build();
//
//        // 修改：添加 uid 权限检查（如果存在uid字段）
//        AddFieldsOperation filterAiTools = Aggregation.addFields()
//                .addFieldWithValue("aiTools",
//                        new Document("$filter",
//                                new Document("input", "$aiTools")
//                                        .append("as", "item")
//                                        .append("cond", new Document("$and", Arrays.asList(
//                                                // 条件1：工具可见性
//                                                new Document("$eq", Arrays.asList("$$item.show", true)),
//                                                // 条件2：用户权限检查（如果存在uid字段）
//                                                new Document("$or", Arrays.asList(
//                                                        // 子条件1：不存在uid字段 -> 直接通过
//                                                        new Document("$eq", Arrays.asList(
//                                                                new Document("$ifNull", Arrays.asList("$$item.uid", null)),
//                                                                null
//                                                        )),
//                                                        // 子条件2：存在uid字段且匹配
//                                                        new Document("$eq", Arrays.asList("$$item.uid", uid))
//                                                ))
//                                        ))))).build();
//
//        // Step 3: Project the required fields and include the newId in bItems
//        ProjectionOperation project = Aggregation.project("name", "aiTools","show");
//        MatchOperation match = Aggregation.match(Criteria.where("show").is(true));
//
//        List<AggregationOperation> operationList = Arrays.asList();
//        operationList.add(addFieldsOperation);
//        operationList.add(lookupOperation);
//// 先执行过滤
//        operationList.add(filterAiTools);  // 先过滤数组
//        operationList.add(convertIdsToString); // 再添加字段
//        operationList.add(project);
//        operationList.add(match);
//
//        Aggregation agg = Aggregation.newAggregation(operationList);
//        // collectionName:主表
//        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, AiToolsGroup.class, HashMap.class);
//        return aggregationResults.getMappedResults();
        return null;
    }

    @Override
    public HashMap queryByUid(String uid) {
        Criteria criteria1 = new Criteria().andOperator(
                // 原有条件：匹配没有 uid 字段或有 uid 字段且值匹配的文档
                new Criteria().orOperator(
                        Criteria.where("uid").exists(false),
                        new Criteria().andOperator(
                                Criteria.where("uid").exists(true),
                                Criteria.where("uid").is(uid)
                        )
                ),
                // 新增条件：show 字段必须为 true
                Criteria.where("show").is(true)
        );
        Query query1 = new Query(criteria1);
        query1.with(Sort.by(Sort.Direction.ASC, "sort"));

        List<AiTools> aiTools = mongoTemplate.find(query1, AiTools.class);

        // 修改后（只返回 show=true 的文档）：
        Criteria showCriteria = Criteria.where("show").is(true);
        Query query2 = new Query(showCriteria);
        query2.with(Sort.by(Sort.Direction.ASC, "sort"));
        List<AiToolsGroup> aiToolsGroup = mongoTemplate.find(query2, AiToolsGroup.class);

        HashMap result = new HashMap();
        result.put("aiTools", aiTools);
        result.put("aiToolsGroup", aiToolsGroup);
        return result;
    }

    @Override
    public AiTools query(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.findOne(query, AiTools.class);
    }

    @Override
    public List<HashMap> queryAll() {
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();

        LookupOperation lookupOperation = Aggregation.lookup("aiTools", "_id", "groupId", "aiTools");

        // Step 2: Use $addFields with $map to convert _id in aiTools to string
        // 将数组中的id转化成新字段
        AddFieldsOperation convertIdsToString = Aggregation.addFields()
                .addFieldWithValue("aiTools",
                        new Document("$map",
                                new Document("input", "$aiTools")
                                        .append("as", "item")
                                        .append("in",
                                                new Document("$mergeObjects", List.of(
                                                        "$$item",
                                                        new Document("aiToolId", new Document("$toString", "$$item._id"))
                                                ))))).build();

        // 修改：添加 uid 权限检查（如果存在uid字段）
        AddFieldsOperation filterAiTools = Aggregation.addFields()
                .addFieldWithValue("aiTools",
                        new Document("$filter",
                                new Document("input", "$aiTools")
                                        .append("as", "item")
                                        .append("cond", new Document("$eq", List.of("$$item.show", true))))).build();

        // Step 3: Project the required fields and include the newId in bItems
        ProjectionOperation project = Aggregation.project("name", "aiTools","show");
        MatchOperation match = Aggregation.match(Criteria.where("show").is(true));

        List<AggregationOperation> operationList = Arrays.asList();
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(convertIdsToString);
        operationList.add(filterAiTools);
        operationList.add(project);
        operationList.add(match);

        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, AiToolsGroup.class, HashMap.class);
        return aggregationResults.getMappedResults();
    }
}
