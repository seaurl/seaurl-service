package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.Report;
import com.seaurl.platform.space.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean addReport(Report report) {
        mongoTemplate.insert(report);
        return true;
    }

    @Override
    public List<Report> queryReportUrlByUserId(String uid, List<String> urlId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("urlId").in(urlId));
        return mongoTemplate.find(query, Report.class);
    }
}
