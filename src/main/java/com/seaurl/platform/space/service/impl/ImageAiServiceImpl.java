package com.seaurl.platform.space.service.impl;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisListResult;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.ChatAiMessage;
import com.seaurl.platform.space.model.ImageAi;
import com.seaurl.platform.common.model.NoteCategory;
import com.seaurl.platform.space.model.QueryResults2;
import com.seaurl.platform.space.service.ImageAiService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import com.seaurl.platform.space.utils.XDateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;

@Service
public class ImageAiServiceImpl implements ImageAiService {

    private Logger log = LoggerFactory.getLogger(getClass());


    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ImageSynthesisResult basicCall(String prompt, String size, Integer n) throws ApiException, NoApiKeyException {
        ImageSynthesis is = new ImageSynthesis();
        ImageSynthesisParam param =
                ImageSynthesisParam.builder()
                        .model(ImageSynthesis.Models.WANX_V1)
                        .n(n)
                        .size(size)
                        .prompt(prompt)
                        .build();

        ImageSynthesisResult result = is.call(param);
        log.info("ImageSynthesisResult ={}", result);
        return result;
    }

    @Override
    public ImageAi add(ImageAi imageAi) {
        return mongoTemplate.save(imageAi);
    }

    @Override
    public ImageAi queryByImageAiId(String imageAiId) {
        Query query = new Query(Criteria.where("imageAiId").is(imageAiId));
        query.with(Sort.by(Sort.Order.desc("createdDt")));
        return mongoTemplate.findOne(query, ImageAi.class);
    }

    @Override
    public boolean editImage(String imageAiId, String imageSrc) {
        Date date = new Date();
        Query query = new Query(Criteria.where("imageAiId").is(imageAiId));
        Update update = new Update();
        update.set("imageSrc", imageSrc);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ImageAi.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editImage(String imageAiId, String imageSrc, Long size) {
        Date date = new Date();
        Query query = new Query(Criteria.where("imageAiId").is(imageAiId));
        Update update = new Update();
        update.set("imageSrc", imageSrc);
        update.set("size", size);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ImageAi.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editImage(String imageAiId, String imageSrc, String imagePath, Long size) {
        Date date = new Date();
        Query query = new Query(Criteria.where("imageAiId").is(imageAiId));
        Update update = new Update();
        update.set("imageSrc", imageSrc);
        update.set("imagePath", imagePath);
        update.set("size", size);
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ImageAi.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editImage(String imageAiId, String name, String imageSrc, String imagePath, Long size) {
        Date date = new Date();
        Query query = new Query(Criteria.where("imageAiId").is(imageAiId));
        Update update = new Update();
        update.set("imageSrc", imageSrc);
        update.set("name", name);
        update.set("imagePath", imagePath);
        update.set("size", size);
        // 设置成成功
        update.set("status", "1");
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ImageAi.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public QueryResults2 queryMessageByPage(String uid, Integer pageNum, Integer pageSize) {
        Criteria criteria1 = Criteria.where("uid").is(uid);

        Query query1 = new Query(criteria1);
        Query query2 = new Query(criteria1);
        query1.with(Sort.by(Sort.Order.desc("createdDt")));
        query1.skip((long) pageSize * (pageNum - 1)).limit(pageSize);

        long total = mongoTemplate.count(query2, ImageAi.class);
        List<ImageAi> results = mongoTemplate.find(query1, ImageAi.class);

        QueryResults2<ImageAi> queryResults = new QueryResults2<>();
        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }

    @Override
    public QueryResults2 queryMessageByPage(String uid, String flagCreatedTime, Integer pageNum, Integer pageSize) {
        Criteria criteria1 = Criteria.where("uid").is(uid);

        // 将毫秒13位时间戳转换成Date类型
        Date pd = XDateUtils.isoDateStrToDate(flagCreatedTime);
        criteria1.and("createdDt").lte(pd);
        Query query1 = new Query(criteria1);
        Query query2 = new Query(criteria1);
        query1.with(Sort.by(Sort.Order.desc("createdDt")));
        query1.skip((long) pageSize * (pageNum - 1)).limit(pageSize);

        long total = mongoTemplate.count(query2, ImageAi.class);
        List<ImageAi> results = mongoTemplate.find(query1, ImageAi.class);

        QueryResults2<ImageAi> queryResults = new QueryResults2<>();
        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }

    @Override
    public boolean removeByImageAiId(String uid, String imageAiId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("imageAiId").is(imageAiId));
        DeleteResult result = mongoTemplate.remove(query, ImageAi.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean removeById(String uid, String id) {
        Query query = new Query(Criteria.where("uid").is(uid).and("id").is(id));
        DeleteResult result = mongoTemplate.remove(query, ImageAi.class);
        return result.getDeletedCount() > 0;
    }

    public void listTask() throws ApiException, NoApiKeyException {
        ImageSynthesis is = new ImageSynthesis();
        AsyncTaskListParam param = AsyncTaskListParam.builder().build();
        ImageSynthesisListResult result = is.list(param);
        System.out.println(result);
    }

    public void fetchTask() throws ApiException, NoApiKeyException {
        String taskId = "your task id";
        ImageSynthesis is = new ImageSynthesis();
        // If set DASHSCOPE_API_KEY environment variable, apiKey can null.
        ImageSynthesisResult result = is.fetch(taskId, null);
        System.out.println(result.getOutput());
        System.out.println(result.getUsage());
    }
}
