package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.model.Report;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.request.ReportRequest;
import com.seaurl.platform.space.service.ReportService;
import com.seaurl.platform.space.service.UserService;
import com.seaurl.platform.space.service.facade.UsersFacade;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/space")

@Observed
public class ReportController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    ReportService reportService;

    @Autowired
    UserService userService;

    @Autowired
    UsersFacade usersFacade;

    /**
     * 根据userid获取用户点赞的网址url列表
     * @param reportRequest
     * @return
     */
    @PostMapping("/crud/report/url/queryByUserId")
    public ResponseEntity<?> queryLikeUrl(@RequestBody ReportRequest reportRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (reportRequest.getUrlIds().length <= 0) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            List<Report> reports = reportService.queryReportUrlByUserId(loginUser.getUid(), Arrays.asList(reportRequest.getUrlIds()));
            List<String> urlIds = new ArrayList<>();
            reports.forEach(item -> urlIds.add(item.getUrlId()));
            return ResponseEntity.ok(new ResultSuccess<>(urlIds));
        }catch (Exception ex) {
            log.error("queryLikeUrl exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 举报链接
     * @param reportRequest
     * @return
     */
    @PostMapping("/crud/report/url")
    public ResponseEntity<?> addReport(@RequestBody ReportRequest reportRequest){
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String urlId = reportRequest.getUrlId();
            String byUid = reportRequest.getByUid();
            if (!StringUtils.hasLength(urlId) || !StringUtils.hasLength(byUid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            Report report = new Report();
            Date date = new Date();
            report.setUid(loginUser.getUid());
            report.setUrlId(urlId);
            report.setReportedUid(byUid);
            report.setCreatedDt(date);
            report.setUpdatedDt(date);
            reportService.addReport(report);
            return ResponseEntity.ok(new ResultSuccess<>(true));
        }catch (Exception ex) {
            log.error("addReport exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
