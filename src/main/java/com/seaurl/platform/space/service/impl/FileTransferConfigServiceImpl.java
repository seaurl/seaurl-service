package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.Domain;
import com.seaurl.platform.space.model.FileTransfer;
import com.seaurl.platform.space.model.FileTransferConfig;
import com.seaurl.platform.common.model.NoteCategory;
import com.seaurl.platform.space.service.FileTransferConfigService;
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class FileTransferConfigServiceImpl implements FileTransferConfigService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<HashMap> queryBy(String uid) {

        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("categoryId").withValue(ConvertOperators.ToObjectId.toObjectId("$categoryId")).build();
        LookupOperation lookupOperation = Aggregation.lookup("fileCategory", "categoryId", "_id", "fileCategory_as");

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid));
        ProjectionOperation project = Aggregation.project("uid", "createdDt", "updatedDt")
                .and(ConvertOperators.ToString.toString("$categoryId")).as("categoryId")
                .and("fileCategory_as.name").as("categoryName");

        UnwindOperation unwind = Aggregation.unwind("fileCategory_as");

        Aggregation agg = Aggregation.newAggregation(addFieldsOperation, lookupOperation, unwind, project, match);

        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, FileTransferConfig.class, HashMap.class);

        return outputType.getMappedResults();
    }

    @Override
    public FileTransferConfig query(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.findOne(query, FileTransferConfig.class);
    }

    @Override
    public FileTransferConfig add(FileTransferConfig fileTransferConfig) {
        return mongoTemplate.insert(fileTransferConfig);
    }

    @Override
    public boolean update(FileTransferConfig fileTransferConfig) {
        Query query = new Query(Criteria.where("uid").is(fileTransferConfig.getUid()));
        Update update = new Update();
        if (StringUtils.hasLength(fileTransferConfig.getPid())){
            update.set("pid", fileTransferConfig.getPid());
        }else{
            update.unset("pid");
        }

        update.set("updatedDt", fileTransferConfig.getUpdatedDt());
        UpdateResult result = mongoTemplate.updateFirst(query, update, FileTransferConfig.class);
        return result.getModifiedCount() > 0;
    }
}
