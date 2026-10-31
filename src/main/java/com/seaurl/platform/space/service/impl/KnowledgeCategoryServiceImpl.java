package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.KnowledgeCategory;
import com.seaurl.platform.space.service.KnowledgeCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class KnowledgeCategoryServiceImpl implements KnowledgeCategoryService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public KnowledgeCategory create(KnowledgeCategory category) {
        return mongoTemplate.save(category);
    }

    @Override
    public KnowledgeCategory getById(String id, String uid) {
        Query query = new Query(Criteria.where("id").is(id).and("uid").is(id).and("isDelete").is(false));
        return mongoTemplate.findOne(query, KnowledgeCategory.class);
    }

    @Override
    public List<KnowledgeCategory> getAll(String uid) {
        Query query = new Query(Criteria.where("isDelete").is(false).and("uid").is(uid));
        return mongoTemplate.find(query, KnowledgeCategory.class);
    }

    @Override
    public Boolean update(KnowledgeCategory category) {
        Query query = Query.query(Criteria.where("id").is(category.getId()));
        Update update = new Update();

        update.set("name", category.getName());
        update.set("desc", category.getDesc());
        update.set("updatedDt", new Date());

        long count1 = mongoTemplate.updateFirst(query, update, KnowledgeCategory.class).getModifiedCount();
        return count1 > 0;
    }

    @Override
    public Boolean delete(String id) {
        try {
            // Soft delete approach
            Query query = new Query(Criteria.where("id").is(id));
            Update update = new Update().set("isDelete", true).set("updatedDt", new Date());
            long count = mongoTemplate.updateFirst(query, update, KnowledgeCategory.class).getModifiedCount();
            return count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public List<KnowledgeCategory> findAllByOrderBySortAsc() {
        return null;
    }
}
