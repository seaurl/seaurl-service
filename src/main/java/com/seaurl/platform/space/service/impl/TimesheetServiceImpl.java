package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.Timesheet;
import com.seaurl.platform.space.service.TimesheetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class TimesheetServiceImpl implements TimesheetService {

    private static Logger log = LoggerFactory.getLogger(TimesheetServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<Timesheet> queryBy(String uid, Integer year, Integer month) {
        Query query = new Query(Criteria.where("uid").is(uid)
                .and("year").is(year)
                .and("month").is(month));
        return mongoTemplate.find(query, Timesheet.class);
    }

    @Override
    public List<Timesheet> search(String uid, String keyword, Integer year, Integer month, Boolean global) {
        Criteria base = Criteria.where("uid").is(uid);
        if (StringUtils.hasLength(keyword)) {
            base = base.and("name").regex(keyword, "i");
        }
        if (!Boolean.TRUE.equals(global)) {
            if (year != null) base = base.and("year").is(year);
            if (month != null) base = base.and("month").is(month);
        }
        Query query = new Query(base);
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedDt"));
        query.limit(200);
        return mongoTemplate.find(query, Timesheet.class);
    }

    @Override
    public List<String> categories(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        query.fields().include("category");
        List<Timesheet> all = mongoTemplate.find(query, Timesheet.class);
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        for (Timesheet t : all) {
            if (t.getCategory() != null && !t.getCategory().trim().isEmpty()) {
                set.add(t.getCategory().trim());
            }
        }
        return new java.util.ArrayList<>(set);
    }

    @Override
    public Timesheet addTask(String uid, String name, Integer year, Integer month, Integer status, Double outputValue, String category, Double totalHours) {
        Date date = new Date();
        Timesheet timesheet = new Timesheet();
        timesheet.setUid(uid);
        timesheet.setName(name);
        timesheet.setYear(year);
        timesheet.setMonth(month);
        timesheet.setHours(new java.util.HashMap<>());
        timesheet.setOutputValues(new java.util.HashMap<>());
        timesheet.setStatus(status != null && status >= 0 && status <= 3 ? status : 0);
        timesheet.setOutputValue(outputValue != null ? outputValue : 0.0);
        timesheet.setCategory(category != null ? category : "");
        timesheet.setTotalHours(totalHours);
        timesheet.setCreatedDt(date);
        timesheet.setUpdatedDt(date);
        return mongoTemplate.insert(timesheet);
    }

    @Override
    public boolean updateHour(String taskId, Integer day, Double hours) {
        Query query = new Query(Criteria.where("id").is(taskId));
        Update update = new Update();
        String fieldKey = "hours." + day;
        if (hours != null) {
            update.set(fieldKey, hours);
        } else {
            update.unset(fieldKey);
        }
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Timesheet.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean updateOutput(String taskId, Integer day, Double value) {
        if (day == null) {
            return false;
        }
        Query query = new Query(Criteria.where("id").is(taskId));
        Timesheet existing = mongoTemplate.findOne(query, Timesheet.class);
        if (existing == null) {
            return false;
        }
        java.util.Map<String, Double> outputValues = existing.getOutputValues();
        if (outputValues == null) {
            outputValues = new java.util.HashMap<>();
        }
        if (value != null) {
            outputValues.put(String.valueOf(day), value);
        } else {
            outputValues.remove(String.valueOf(day));
        }
        Update update = new Update();
        update.set("outputValues", outputValues);
        double sum = outputValues.values().stream().mapToDouble(v -> v == null ? 0 : v).sum();
        update.set("outputValue", sum);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Timesheet.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean deleteTask(String taskId) {
        Query query = new Query(Criteria.where("id").is(taskId));
        DeleteResult result = mongoTemplate.remove(query, Timesheet.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean renameTask(String taskId, String name) {
        Query query = new Query(Criteria.where("id").is(taskId));
        Update update = new Update();
        update.set("name", name);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Timesheet.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean updateStatus(String taskId, Integer status) {
        if (status == null || status < 0 || status > 3) {
            return false;
        }
        Query query = new Query(Criteria.where("id").is(taskId));
        Update update = new Update();
        update.set("status", status);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Timesheet.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean updateTask(String taskId, String name, Integer status, Double outputValue, String category, Double totalHours) {
        Query query = new Query(Criteria.where("id").is(taskId));
        Update update = new Update();
        if (name != null) {
            update.set("name", name);
        }
        if (status != null && status >= 0 && status <= 3) {
            update.set("status", status);
        }
        if (outputValue != null) {
            update.set("outputValue", outputValue);
        }
        if (category != null) {
            update.set("category", category);
        }
        if (totalHours != null) {
            update.set("totalHours", totalHours);
        }
        if (update.getUpdateObject().isEmpty()) {
            return false;
        }
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Timesheet.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Integer copyLastMonth(String uid, Integer year, Integer month) {
        int prevYear = year;
        int prevMonth = month - 1;
        if (prevMonth < 1) {
            prevMonth = 12;
            prevYear = year - 1;
        }

        Query existQuery = new Query(Criteria.where("uid").is(uid)
                .and("year").is(year)
                .and("month").is(month));
        long existCount = mongoTemplate.count(existQuery, Timesheet.class);
        if (existCount > 0) {
            return 0;
        }

        Query prevQuery = new Query(Criteria.where("uid").is(uid)
                .and("year").is(prevYear)
                .and("month").is(prevMonth));
        List<Timesheet> prevTasks = mongoTemplate.find(prevQuery, Timesheet.class);
        if (prevTasks.isEmpty()) {
            return 0;
        }

        Date date = new Date();
        int copied = 0;
        for (Timesheet prev : prevTasks) {
            Timesheet t = new Timesheet();
            t.setUid(uid);
            t.setName(prev.getName());
            t.setYear(year);
            t.setMonth(month);
            t.setHours(new java.util.HashMap<>());
            t.setOutputValues(new java.util.HashMap<>());
            t.setStatus(0);
            t.setOutputValue(prev.getOutputValue() != null ? prev.getOutputValue() : 0.0);
            t.setCategory(prev.getCategory() != null ? prev.getCategory() : "");
            t.setTotalHours(prev.getTotalHours());
            t.setCreatedDt(date);
            t.setUpdatedDt(date);
            mongoTemplate.insert(t);
            copied++;
        }
        return copied;
    }
}
