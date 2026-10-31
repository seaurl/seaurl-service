package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.File;
import com.seaurl.platform.common.model.FileShare;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.service.FileShareService;
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

import java.util.*;

@Service
public class FileShareServiceImpl implements FileShareService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public FileShare add(FileShare fileShare) {
        return mongoTemplate.insert(fileShare);
    }

    @Override
    public boolean isExist(String uid, String fileId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("fileId").is(fileId));
        FileShare fileShare = mongoTemplate.findOne(query, FileShare.class);
        return fileShare != null;
    }

    @Override
    public boolean update(FileShare fileShare) {
        Query query = new Query(Criteria.where("uid").is(fileShare.getUid()).and("fileId").is(fileShare.getFileId()));
        Update update = new Update();
        update.addToSet("friendIds", fileShare.getFriendIds().get(0));
        UpdateResult result = mongoTemplate.updateFirst(query, update, FileShare.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean deleteFriend(String uid, String fileId, String fid) {
        log.info("fileId = {},fid = {}", fileId, fid);
        Query query = new Query(Criteria.where("uid").is(uid).and("fileId").is(fileId));
        Update update = new Update();
        update.pull("friendIds", fid);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, FileShare.class);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public List<User> queryShareFileFriends(String uid, String fileId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("fileId").is(fileId));
        FileShare fileShare = mongoTemplate.findOne(query, FileShare.class);
        if (fileShare == null) {
            return null;
        } else {
            List<String> fids = fileShare.getFriendIds();
            Query query2 = new Query(Criteria.where("uid").in(fids).and("status").is(2));
            return mongoTemplate.find(query2, User.class);
        }

    }

    @Override
    public FileShare queryBy(String uid, String fileId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("fileId").is(fileId));
        return mongoTemplate.findOne(query, FileShare.class);
    }

    @Override
    public List<HashMap> queryMyShareFileList(String uid) {
        // 1、添加uid字段类型转换
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("fileId").withValue(ConvertOperators.ToObjectId.toObjectId("$fileId")).build();

        // 2、按参数顺序：被关联表user，主表apple.uid，被关联表user.id，别名
        LookupOperation lookupOperation = Aggregation.lookup("file", "fileId", "_id", "file_as");

        // 3、查询哪些字段，类似sql里面的 select 选择器
        // 注意：因为user是被关联表，所以要使用user_as别名的方式来获取用户信息字段
        ProjectionOperation project = Aggregation.project( "friendIds","file_as.uid", "file_as.pid", "file_as.name", "file_as.type", "file_as.size", "file_as.url", "file_as.path", "file_as.fullPath", "file_as.isTrashbin", "file_as.isFavorite", "file_as.isDelete", "file_as.createdDt", "file_as.updatedDt")
                .and("updatedDt").as("sortDt")
                .andExpression("toString(file_as._id)").as("id");

        // 4、按照apple.device数量排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "sortDt");

        Criteria query = Criteria.where("uid").in(uid)
                .and("file_as.isDelete").is(false)
                .and("friendIds").ne(Collections.emptyList());

        MatchOperation match = Aggregation.match(query);

        UnwindOperation unwind = Aggregation.unwind("file_as");
        // 5、添加取List中
        List<AggregationOperation> operationList = new ArrayList<>();
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        operationList.add(unwind);
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);
        Aggregation agg = Aggregation.newAggregation(operationList);

// 因为返回的是User实体类，所以不会出现device字段，你可以使用Map.class来返回想要的字段
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, FileShare.class, HashMap.class);
// 6、返回关联查询结果
        return aggregationResults.getMappedResults();
    }
}
