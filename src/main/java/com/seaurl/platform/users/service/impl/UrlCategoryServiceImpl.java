package com.seaurl.platform.users.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.UrlCategory;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.service.UrlCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("usersUrlCategoryServiceImpl")
public class UrlCategoryServiceImpl implements UrlCategoryService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean addCategory(UrlCategory urlCategory) {
        mongoTemplate.insert(urlCategory);
        return true;
    }

    @Override
    public boolean addSubCategory(String uid, String pid, String name) {
        UrlCategory urlCategory = new UrlCategory();
        urlCategory.setUid(uid);
        urlCategory.setPid(pid);
        urlCategory.setName(name);
        mongoTemplate.insert(urlCategory);
        return true;
    }

    @Override
    public boolean editCategoryName(String categoryId, String categoryName) {
        Query query = new Query(Criteria.where("id").is(categoryId));
        Update update = new Update();
        update.set("name", categoryName);
        UpdateResult result = mongoTemplate.updateFirst(query, update, UrlCategory.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean deleteCategory(String categoryId) {
        Query query = new Query(Criteria.where("id").is(categoryId));
        DeleteResult result = mongoTemplate.remove(query, UrlCategory.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public List<UrlCategory> queryCategoryByUserName(String userName) {
        Query query = new Query(Criteria.where("userName").is(userName));
        User user = mongoTemplate.findOne(query, User.class);
        Query query2 = new Query(Criteria.where("uid").is(user.getUid()).and("pid").exists(false));
        return mongoTemplate.find(query2, UrlCategory.class);
    }

    @Override
    public List<UrlCategory> queryCategoryByPid(String pid) {
        Query query = new Query(Criteria.where("pid").is(pid));
        return mongoTemplate.find(query, UrlCategory.class);
    }

    @Override
    public List<UrlCategory> queryCategoryByUid(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.find(query, UrlCategory.class);
    }
}
