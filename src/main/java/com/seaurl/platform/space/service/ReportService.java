package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.Report;

import java.util.List;

public interface ReportService {

    boolean addReport(Report report);

    List<Report> queryReportUrlByUserId(String uid, List<String> urlId);
}
