package com.seaurl.platform.users.controller;

import com.alibaba.fastjson.JSON;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContextHolder;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.model.UserPoints;
import com.seaurl.platform.users.request.UserSubmitRequest;
import com.seaurl.platform.users.service.DashboardService;
import com.seaurl.platform.users.service.UrlService;
import com.seaurl.platform.users.service.UserPointsService;
import com.seaurl.platform.users.service.UserService;
import com.seaurl.platform.users.service.UserWatchService;
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
import java.util.HashMap;
import java.util.List;

@RestController("usersUrlController")
@RequestMapping("/users")

@Observed
public class UrlController {

    private static Logger log = LoggerFactory.getLogger(UrlController.class);

    @Autowired
    UserService userService;

    @Autowired
    UserWatchService userWatchService;

    @Autowired
    UrlService urlService;

    @Autowired
    UserPointsService userPointsService;

    @Autowired
    DashboardService dashboardService;

    /**
     * 根据年份获取提交的数据
     *
     * @param userSubmitRequest
     * @return
     */
    @PostMapping("/crud/getHeatMapSubmitData")
    public ResponseEntity<?> getHeatMapSubmitData(@RequestBody UserSubmitRequest userSubmitRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null || !StringUtils.hasLength(loginUser.getUserName())) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            User user = userService.getUserByName(loginUser.getUserName());
            if (user == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            String uid = user.getUid();

            String date = userSubmitRequest.getDate();
            String type = userSubmitRequest.getType();
            if (!StringUtils.hasLength(date)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            List<HashMap> heatmapSubmitData = new ArrayList<>();
            if ("currentDate".equals(type)) {
                heatmapSubmitData = dashboardService.getHeatMapSubmitDataByCurrentYear(uid);
            }
            if ("fullYear".equals(type)) {
                String[] dateParts = date.split("-");
                heatmapSubmitData = dashboardService.getHeatMapSubmitDataByYear(uid, dateParts[0]);
            }

            List<Integer> yearList = dashboardService.getSubmitYearList(user);

            // 详情：当前月当天，四类聚合（url/note/file/mail），单元素数组以兼容前端 push/replace
            List<HashMap> dateList = dashboardService.getActivityByDate(uid, date, "y-m");

            HashMap<String, Object> data = new HashMap<>();
            data.put("heatmapSubmitData", heatmapSubmitData);
            data.put("yearList", yearList);
            data.put("dateList", dateList);
            return ResponseEntity.ok(new ResultSuccess<>(data));
        } catch (Exception ex) {
            log.error("getHeatMapSubmitData exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param userSubmitRequest
     * @return
     */
    @PostMapping("/crud/getSubmitUrlByDate")
    public ResponseEntity<?> getSubmitUrlByDate(@RequestBody UserSubmitRequest userSubmitRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null || !StringUtils.hasLength(loginUser.getUserName())) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            User user = userService.getUserByName(loginUser.getUserName());
            if (user == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            String uid = user.getUid();

            String date = userSubmitRequest.getDate();
            String format = userSubmitRequest.getFormat();
            if (!StringUtils.hasLength(date) || !StringUtils.hasLength(format)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            // 四类聚合，外层为单元素数组以兼容前端 push/replace
            List<HashMap> dateDataList = dashboardService.getActivityByDate(uid, date, format);
            return ResponseEntity.ok(new ResultSuccess<>(dateDataList));
        } catch (Exception ex) {
            log.error("getSubmitUrlByDate exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
