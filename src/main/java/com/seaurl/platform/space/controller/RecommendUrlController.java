package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.service.UrlService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/space")

@Observed
public class RecommendUrlController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    UrlService urlService;

    /**
     * 获取用户喜欢的网址top10
     * @return
     */
    @GetMapping("/crud/url/getTop10LikeUrlsInWeek")
    public ResponseEntity<?> getTop10LikeUrlsInWeek() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            List<HashMap> urls = urlService.top10LikeUrlsInWeek();
            return ResponseEntity.ok(new ResultSuccess<>(urls));
        }catch (Exception ex) {
            log.error("getTop10LikeUrlsInWeek exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取用户点击最多的网址top10
     * @return
     */
    @GetMapping("/crud/url/getTop10ClickUrlsInWeek")
    public ResponseEntity<?> getTop10ClickUrlsInWeek() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            List<HashMap> urls = urlService.top10ClickUrlsInWeek();
            return ResponseEntity.ok(new ResultSuccess<>(urls));
        }catch (Exception ex) {
            log.error("getTop10ClickUrlsInWeek exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
