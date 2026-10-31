package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.seaurl.platform.space.model.FileTransfer;
import com.seaurl.platform.space.service.FileTransferService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class FileTransferServiceImpl implements FileTransferService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public FileTransfer add(FileTransfer fileTransfer) {
        return mongoTemplate.insert(fileTransfer);
    }

    @Override
    public boolean delete(String fileTransferId) {
        Query query = new Query(Criteria.where("id").is(fileTransferId));
        DeleteResult result = mongoTemplate.remove(query, FileTransfer.class);
        return result.getDeletedCount() > 0;
    }

    /**
     * @param uid
     * @param date
     * @return
     */
    @Override
    public List<HashMap> queryBy(String uid, String date) {
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("fileId").withValue(ConvertOperators.ToObjectId.toObjectId("$fileId")).build();
        LookupOperation lookupOperation = Aggregation.lookup("file", "fileId", "_id", "file_as");

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid)
                .and("actionDate").is(date));
        // 筛选数组中show=true的数据
        AddFieldsOperation filter = Aggregation.addFields()
                .addFieldWithValue("file_as",
                        new Document("$filter",
                                new Document("input", "$file_as")
                                        .append("as", "item")
                                        .append("cond", new Document("$eq", List.of("$$item.isDelete", false))))).build();

        ProjectionOperation project = Aggregation.project("uid", "aiToolId", "content", "type", "fileId", "createdDt", "updatedDt", "file_as")
                .andExpression("{ $dateToString: {date: '$actionDate', format: '%Y-%m-%d', timezone: '+08:00'}}").as("actionDate")
                .and(ConvertOperators.ToString.toString("$fileId")).as("fileId")
                .and(ConvertOperators.ToString.toString("$_id")).as("id");
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "createdDt");
//        UnwindOperation unwind = Aggregation.unwind("file_as");
        Aggregation agg = Aggregation.newAggregation(addFieldsOperation, lookupOperation, filter, project, sort, match);

        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, FileTransfer.class, HashMap.class);

        return outputType.getMappedResults();
    }

    @Override
    public List<HashMap> queryByMonth(String uid, String dateMonth) {
        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid).and("actionDateMonth").is(dateMonth));
        ProjectionOperation project = Aggregation.project("uid", "aiToolId", "content", "type", "fileId", "createdDt", "updatedDt")
                .andExpression("{ $dateToString: {date: '$actionDate', format: '%Y-%m', timezone: '+08:00'}}").as("actionDateMonth")
                .andExpression("{ $dateToString: {date: '$actionDate', format: '%Y-%m-%d', timezone: '+08:00'}}").as("actionDate");

        // 根据actionDate分组并计算每组的数量
        GroupOperation group = Aggregation.group("actionDate")
                .first("uid").as("uid")
                .first("aiToolId").as("aiToolId")
                .first("createdDt").as("createdDt")
                .first("updatedDt").as("updatedDt")
                .count().as("count");

        // 重命名_id为actionDate
        ProjectionOperation projectAfterGroup = Aggregation.project("uid", "aiToolId", "createdDt", "updatedDt", "count")
                .and("$_id").as("actionDate")
                .andExclude("_id");  // 排除_id字段

        SortOperation sort = Aggregation.sort(Sort.Direction.ASC, "createdDt");
        Aggregation agg = Aggregation.newAggregation(project, match, group, projectAfterGroup, sort);

        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, FileTransfer.class, HashMap.class);

        return outputType.getMappedResults();
    }
}
