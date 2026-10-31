package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.NoteCategory;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.service.NoteCategoryService;
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
public class NoteCategoryServiceImpl implements NoteCategoryService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean dragCategory(String uid, String categoryId, String targetId, String type) {
        Query query1 = new Query(Criteria.where("id").is(categoryId));
        NoteCategory noteCategory = mongoTemplate.findOne(query1, NoteCategory.class);

        boolean isAfter = type.equals("after");
        boolean isBefore = type.equals("before");
        boolean isInner = type.equals("inner");

        if (noteCategory != null) {
            String pid = noteCategory.getPid();
            Query query2;
            if (StringUtils.hasLength(pid)) {
                query2 = new Query(Criteria.where("pid").is(pid).and("sort").gt(noteCategory.getSort()));
            } else {
                query2 = new Query(Criteria.where("uid").is(uid).orOperator(Criteria.where("pid").exists(false), Criteria.where("pid").is(null)).and("sort").gt(noteCategory.getSort()));
            }
            Update update1 = new Update();
            //1、 先将，之前节点级别之后所有节点order num - 1
            update1.inc("sort", -1);
            mongoTemplate.updateMulti(query2, update1, NoteCategory.class);


            // 2、然后，目标级别节点之后所有节点order num + 1
            Query query3 = new Query(Criteria.where("id").is(targetId));
            NoteCategory tartgetCategory = mongoTemplate.findOne(query3, NoteCategory.class);

            if (tartgetCategory == null) {
                return false;
            }

            //
            if (isAfter || isBefore) {

                Query query4 = null;
                if (StringUtils.hasLength(tartgetCategory.getPid())) {
                    // 如果是after 则把after之后的所有节点 sort+1
                    if (isAfter) {
                        query4 = new Query(Criteria.where("pid").is(tartgetCategory.getPid()).and("sort").gt(tartgetCategory.getSort()));
                    }
                    // 如果是before 则把before和before之后的所有节点 sort+1
                    if (isBefore) {
                        query4 = new Query(Criteria.where("pid").is(tartgetCategory.getPid()).and("sort").gte(tartgetCategory.getSort()));
                    }

                } else {
                    // 如果是after 则把after之后的所有节点 sort+1
                    if (isAfter) {
                        query4 = new Query(Criteria.where("uid").is(uid).orOperator(Criteria.where("pid").exists(false), Criteria.where("pid").is(null)).and("sort").gt(tartgetCategory.getSort()));
                    }
                    // 如果是before 则把before和before之后的所有节点 sort+1
                    if (isBefore) {
                        query4 = new Query(Criteria.where("uid").is(uid).orOperator(Criteria.where("pid").exists(false), Criteria.where("pid").is(null)).and("sort").gte(tartgetCategory.getSort()));
                    }
                }
                Update update2 = new Update();
                update2.inc("sort", 1);
                mongoTemplate.updateMulti(query4, update2, NoteCategory.class);
            }

            // 如果是inner，那么只要把targetCategory里面的所有节点sort +1
            if (isInner) {
                Query query5 = new Query(Criteria.where("pid").is(targetId));
                Update update5 = new Update();
                update5.inc("sort", 1);
                mongoTemplate.updateMulti(query5, update5, NoteCategory.class);
            }


            // 3、最后，当前节点 order num = 目标位置上一级order num + 1,pid = 目标位置pid
            // 如果type = before | inner那么order num直接=1
            Update update3 = new Update();
            if (isAfter || isBefore) {
                if (StringUtils.hasLength(tartgetCategory.getPid())) {
                    update3.set("pid", tartgetCategory.getPid());
                } else {
                    update3.set("pid", null);
                }
            }
            if (isInner) {
                update3.set("pid", tartgetCategory.getId());
            }

            if (isAfter) {
                update3.set("sort", tartgetCategory.getSort() + 1);
            } else {
                // 如果是before sort = 0
                update3.set("sort", 0);
            }

            mongoTemplate.updateMulti(query1, update3, NoteCategory.class);
            return true;
        }
        return false;
    }

    @Override
    public NoteCategory getMaxSortCategory(String uid, String parentId) {
        Query query;
        if (!StringUtils.hasLength(parentId)) {
            query = new Query(Criteria.where("uid").is(uid).orOperator(Criteria.where("pid").exists(false), Criteria.where("pid").is(null)));
        } else {
            query = new Query(Criteria.where("uid").is(uid).and("pid").is(parentId));
        }
        query.with(Sort.by(Sort.Order.desc("sort")));
        return mongoTemplate.findOne(query, NoteCategory.class);
    }

    @Override
    public int getMinSortByCategoryId(String uid, String targetId) {
        Query query;
        if (!StringUtils.hasLength(targetId)) {
            query = new Query(Criteria.where("uid").is(uid).orOperator(Criteria.where("pid").exists(false), Criteria.where("pid").is(null)));
        } else {
            query = new Query(Criteria.where("uid").is(uid).and("pid").is(targetId));
        }
        // 查询目标节点
        query.with(Sort.by(Sort.Order.asc("sort")));
        NoteCategory noteCategory = mongoTemplate.findOne(query, NoteCategory.class);
        Date date = new Date();
        if (noteCategory != null) {
            Update update = new Update();
            update.inc("sort", 1);
            update.set("updatedDt", date);
            // 更新目标节点里面的所有子节点sort+1
            mongoTemplate.updateMulti(query, update, NoteCategory.class);
            return noteCategory.getSort();
        } else {
            return 0;
        }
    }

    @Override
    public boolean addCategory(NoteCategory noteCategory) {
        mongoTemplate.insert(noteCategory);
        return true;
    }

    @Override
    public NoteCategory addCategory2(NoteCategory noteCategory) {
        NoteCategory noteCategory1 = mongoTemplate.insert(noteCategory);
        return noteCategory1;
    }

    @Override
    public boolean editCategoryName(String categoryId, String categoryName, String pid) {
        Query query = new Query(Criteria.where("id").is(categoryId));
        Update update = new Update();
        update.set("name", categoryName);
        update.set("pid", pid);
        UpdateResult result = mongoTemplate.updateFirst(query, update, NoteCategory.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean deleteCategory(String categoryId) {
        Query query = new Query(Criteria.where("id").is(categoryId));
        DeleteResult result = mongoTemplate.remove(query, NoteCategory.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public void setCategorySortBy(String uid, String categoryId) {
        Query query;
        if (StringUtils.hasLength(categoryId)) {
            query = new Query(Criteria.where("uid").is(uid).and("pid").is(categoryId));
        } else {
            query = new Query(Criteria.where("uid").is(uid).orOperator(Criteria.where("pid").exists(false), Criteria.where("pid").is(null)));
        }
        Update update = new Update();
        update.inc("sort", 1);
        mongoTemplate.updateMulti(query, update, NoteCategory.class);
    }

    @Override
    public List<NoteCategory> queryCategoryByUserName(String userName) {
        Query query = new Query(Criteria.where("userName").is(userName));
        User user = mongoTemplate.findOne(query, User.class);
        Query query2 = new Query(Criteria.where("uid").is(user.getUid()));
        query2.with(Sort.by(Sort.Order.asc("createdDt")));
        return mongoTemplate.find(query2, NoteCategory.class);
    }

    @Override
    public List<NoteCategory> queryCategoryByPid(String pid) {
        Query query = new Query(Criteria.where("pid").is(pid));
        query.with(Sort.by(Sort.Order.asc("createdDt")));
        return mongoTemplate.find(query, NoteCategory.class);
    }

    @Override
    public List<NoteCategory> queryCategoryByUid(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, NoteCategory.class);
    }

    @Override
    public List<HashMap> queryAllCategoriesAndNoteCountByUid(String uid) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("uid").is(uid)),

                // 将 ObjectId 转成字符串以便 join
                Aggregation.addFields()
                        .addField("_id")
                        .withValue(ConvertOperators.ToString.toString("$_id"))
                        .build(),

                // 关联 Url 表（根据 categoryId ）
                Aggregation.lookup("note", "_id", "categoryId", "notes"),

                // 统计 url 数量
                Aggregation.addFields()
                        .addField("noteCount")
                        .withValue(
                                ArrayOperators.Size.lengthOfArray(
                                        ArrayOperators.Filter.filter("notes")
                                                .as("item")
                                                .by(
                                                        ComparisonOperators.Eq.valueOf("item.isDelete").equalToValue(false)

                                                )
                                )
                        )
                        .build(),
                // 指定返回字段（可选）
                Aggregation.project("name", "pid", "sort", "icon", "updatedDt", "noteCount")
                        .and("_id").as("id"),

                // 按 updatedDt 降序排序
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "updatedDt"))
        );

        return mongoTemplate.aggregate(
                aggregation,
                NoteCategory.class, // 集合名
                HashMap.class
        ).getMappedResults();
    }

    @Override
    public boolean setTop(String cid, Boolean isTop) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(cid));
        Update update = new Update();
        update.set("isTop", isTop);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, NoteCategory.class);
        return result.getModifiedCount() > 0;
    }
}
