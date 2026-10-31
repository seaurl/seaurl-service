package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.FileCategory;
import com.seaurl.platform.common.model.NoteCategory;
import com.seaurl.platform.common.model.UrlCategory;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.service.FileCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class FileCategoryServiceImpl implements FileCategoryService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;


    @Override
    public boolean addCategory(FileCategory fileCategory) {
        mongoTemplate.insert(fileCategory);
        return true;
    }

    @Override
    public FileCategory addCategory2(FileCategory fileCategory) {
        return mongoTemplate.insert(fileCategory);
    }

    @Override
    public boolean editCategoryName(String categoryId, String categoryName, String pid) {
        Query query = new Query(Criteria.where("id").is(categoryId));
        Update update = new Update();
        update.set("name", categoryName);
        update.set("pid", pid);
        UpdateResult result = mongoTemplate.updateFirst(query, update, FileCategory.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean deleteCategory(String categoryId) {
        Query query = new Query(Criteria.where("id").is(categoryId));
        DeleteResult result = mongoTemplate.remove(query, FileCategory.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public List<FileCategory> queryCategoryByUserName(String userName) {
        Query query = new Query(Criteria.where("userName").is(userName));
        User user = mongoTemplate.findOne(query, User.class);
        Query query2 = new Query(Criteria.where("uid").is(user.getUid()));
        query2.with(Sort.by(Sort.Order.asc("updatedDt")));
        return mongoTemplate.find(query2, FileCategory.class);
    }

    @Override
    public List<FileCategory> queryCategoryByPid(String pid) {
        Query query = new Query(Criteria.where("pid").is(pid));
        query.with(Sort.by(Sort.Order.desc("isTop"), Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, FileCategory.class);
    }

    @Override
    public List<FileCategory> queryCategoryByUid(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        query.with(Sort.by(Sort.Order.desc("isTop"), Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, FileCategory.class);
    }

    @Override
    public boolean setTop(String cid, Boolean isTop) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(cid));
        Update update = new Update();
        update.set("isTop", isTop);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, FileCategory.class);
        return result.getModifiedCount() > 0;
    }
}
