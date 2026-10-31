package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.common.model.Note;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.space.service.NoteService;
import com.seaurl.platform.space.utils.DateUtils;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class NoteServiceImpl implements NoteService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    /**
     * 获取最近使用的笔记
     *
     * @param uid
     * @return
     */
    @Override
    public List<Note> getRecentlyNotes(String uid) {
        log.info("------getRecentlyNotes------{}", uid);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();

        // 获取上个月数据
        c.setTime(new Date());
        c.add(Calendar.MONTH, -1);
        Date m = c.getTime();
        String mon = format.format(m);

        ProjectionOperation projectionOperation = Aggregation.project("id", "uid", "categoryId", "title", "isFavorite", "updatedDt")
                .andExpression("substrCP(content, 0, 100)").as("content") // Truncate 'content' field to 100 characters
                .andExpression("toString(id)").as("id");

        MatchOperation matchOperation = Aggregation.match(Criteria.where("uid").is(uid).and("isDelete").is(false).andOperator(Criteria.where("updatedDt").gte(
                // 要使用ISO日期格式的才行，否则 时间会多少算8小时
                DateUtils.dateToISODate(mon)
        )));
        LimitOperation limit = Aggregation.limit(100);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt");
        Aggregation agg = Aggregation.newAggregation(matchOperation, projectionOperation, sort, limit);
        AggregationResults<Note> outputType = mongoTemplate.aggregate(agg, Note.class, Note.class);
        List<Note> dataList = outputType.getMappedResults();
        return dataList;
    }

    @Override
    public Object getRecentlyNotesCount(String uid) {
        log.info("------getRecentlyNotes------{}", uid);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();

        // 获取上个月数据
        c.setTime(new Date());
        c.add(Calendar.MONTH, -1);
        Date m = c.getTime();
        String mon = format.format(m);

        MatchOperation matchOperation = Aggregation.match(Criteria.where("uid").is(uid).and("isDelete").is(false).andOperator(Criteria.where("updatedDt").gte(
                // 要使用ISO日期格式的才行，否则 时间会多少算8小时
                DateUtils.dateToISODate(mon)
        )));
        // 限制返回前12条数据
        Aggregation agg = Aggregation.newAggregation(matchOperation,
                Aggregation.count().as("count"));
        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, Note.class, HashMap.class);
        return Optional.ofNullable(outputType.getUniqueMappedResult())
                .map(result -> result.get("count"))
                .orElse(0);
    }

    @Override
    public boolean add(Note note) throws IOException {
        mongoTemplate.insert(note);
        return true;
    }

    @Override
    public boolean delete(String noteId) {
        Query query = new Query(Criteria.where("id").is(noteId));
        Update update = new Update();
        update.set("isDelete", true);
        UpdateResult result = mongoTemplate.updateFirst(query, update, Note.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editContent(Note note) {
        Query query = new Query(Criteria.where("id").is(note.getId()));
        Update update = new Update();
        update.set("content", note.getContent());
        update.set("updatedDt", note.getUpdatedDt());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Note.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editCategoryId(Note note) {
        Query query = new Query(Criteria.where("id").is(note.getId()));
        Update update = new Update();
        update.set("categoryId", note.getCategoryId());
        update.set("updatedDt", note.getUpdatedDt());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Note.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editTitle(Note note) {
        Query query = new Query(Criteria.where("id").is(note.getId()));
        Update update = new Update();
        if (StringUtils.hasLength(note.getTitle())) {
            update.set("title", note.getTitle());
        }
        update.set("updatedDt", note.getUpdatedDt());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Note.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editIsPrivate(String noteId, Boolean isPrivate) {
        Query query = new Query(Criteria.where("id").is(noteId));
        Update update = new Update();
        update.set("isPrivate", isPrivate);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Note.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Note queryNoteById(String noteId) {
        Query query = new Query(Criteria.where("id").is(noteId).and("isDelete").is(false));
        return mongoTemplate.findOne(query, Note.class);
    }

    @Override
    public List<Note> queryNoteByCategoryId(String categoryId) {
        // Match operation
        AggregationOperation match = Aggregation.match(Criteria.where("categoryId").is(categoryId).and("isDelete").is(false));

        // Projection operation: only include 'content' field with truncated content and other necessary fields
        AggregationOperation project = Aggregation.project()
                .andExpression("substrCP(content, 0, 100)").as("content") // Truncate 'content' field to 100 characters
                .andInclude("title", "categoryId", "isDelete", "isPrivate", "isFavorite", "sort", "createdDt", "updatedDt"); // Include other fields that are necessary

        // Sort operation
        AggregationOperation sort = Aggregation.sort(Sort.by(Sort.Order.desc("updatedDt")));

        // Aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(match, project, sort);

        // Execute aggregation
        AggregationResults<Note> results = mongoTemplate.aggregate(aggregation, Note.class, Note.class);

        return results.getMappedResults();
    }

    @Override
    public List<Note> queryNoteByTitle(String uid, String title) {
        // 模糊匹配
        Pattern pattern = Pattern.compile("^.*" + title + ".*$", Pattern.CASE_INSENSITIVE);
        Criteria criteria1 = Criteria.where("uid").is(uid).and("title").regex(pattern).and("isDelete").is(false);
        Criteria criteria2 = Criteria.where("uid").is(uid).and("domain").regex(pattern).and("isDelete").is(false);
        Query query = new Query(new Criteria().orOperator(criteria1, criteria2));
        return mongoTemplate.find(query, Note.class);
    }

    @Override
    public boolean setFavorite(String noteId, boolean isFavorite) {
        Date date = new Date();
        Query query = new Query(Criteria.where("id").is(noteId));
        Update update = new Update();
        update.set("isFavorite", isFavorite);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, Note.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public List<Note> queryIsFavoriteList(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false).and("isFavorite").is(true));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        query.limit(60); // 限制返回前 20 条数据

        return mongoTemplate.find(query, Note.class);
    }

    @Override
    public Object queryIsFavoriteCount(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false).and("isFavorite").is(true));

        return mongoTemplate.count(query, Note.class);
    }

    @Override
    public List<Note> searchNotes(String uid, String keyword) {
        log.info("------searchNotes------uid={}, keyword={}", uid, keyword);

        Criteria combined;
        if (StringUtils.hasLength(keyword)) {
            Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
            combined = new Criteria().andOperator(
                    Criteria.where("uid").is(uid).and("isDelete").is(false),
                    new Criteria().orOperator(
                            Criteria.where("title").regex(pattern),
                            Criteria.where("content").regex(pattern)
                    )
            );
        } else {
            combined = Criteria.where("uid").is(uid).and("isDelete").is(false);
        }

        MatchOperation matchOperation = Aggregation.match(combined);
        ProjectionOperation projectionOperation = Aggregation.project("id", "uid", "categoryId", "title", "isFavorite", "isPrivate", "updatedDt")
                .andExpression("substrCP(content, 0, 200)").as("content")
                .andExpression("toString(id)").as("id");

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt");
        LimitOperation limit = Aggregation.limit(30);

        Aggregation agg = Aggregation.newAggregation(matchOperation, projectionOperation, sort, limit);
        AggregationResults<Note> results = mongoTemplate.aggregate(agg, Note.class, Note.class);
        return results.getMappedResults();
    }
}
