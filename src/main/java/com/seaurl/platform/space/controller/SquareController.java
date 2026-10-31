package com.seaurl.platform.space.controller;

import com.google.gson.Gson;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.model.QueryResults2;
import com.seaurl.platform.space.request.SquareRequest;
import com.seaurl.platform.space.service.GithubDataService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/space")

public class SquareController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    GithubDataService githubService;

    /**
     * 获取github数据
     *
     * @param squareRequest
     * @return
     */
    @PostMapping("/crud/getGithubData")
    public ResponseEntity<?> getGithubData(@RequestBody SquareRequest squareRequest) {
        String lang = squareRequest.getLang();
        Integer pageNo = squareRequest.getPageNo();
        Integer pageSize = 50;
        String param = squareRequest.getParam();

        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (pageNo == null || !StringUtils.hasLength(lang)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        log.info("lang={}", lang);
        try {

            QueryResults2 result = githubService.queryBy(param, lang,pageNo, pageSize);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getGithubData error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     * @param lang
     * @return
     */
    @GetMapping("/info/getBaiduData/{lang}")
    public String getBaiduData(@PathVariable String lang) {
        if (!StringUtils.hasLength(lang)) {
            return "empty";
        }
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet("https://kaifa.baidu.com/rest/v1/home/github?optionLanguage=" + lang + "&optionSince=MONTHLY");
            CloseableHttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String jsonContent = EntityUtils.toString(entity, "UTF-8");
                return jsonContent;
            } else {
                log.error("Failed to fetch data with status code: {}", response.getStatusLine().getStatusCode());
            }

            response.close();
        } catch (IOException e) {
            log.error("获取百度数据失败：{}", e.getMessage());
            return "error";
        }
        return "error";
    }

    /**
     * 获取msn新闻
     *
     * @param tabId
     * @param pageNo
     * @return
     */
    @GetMapping("/crud/getMsnData/{tabId}/{pageNo}")
    public String getMsnData(@PathVariable String tabId, @PathVariable Integer pageNo) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return "";
        }
        if (pageNo == null || !StringUtils.hasLength(tabId)) {
            return "empty";
        }
        log.info("getMsnData tabId={},pageNo={}", tabId, pageNo);
       
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            CloseableHttpResponse response;
            // 财经新闻
            if (tabId.equals("finance_news")){
                HttpGet httpGet = new HttpGet("https://assets.msn.cn/service/MSN/Feed/me" +
                        "?$skip=" + (pageNo * 48) +
                        "&apikey=0QfOX3Vn51YCzitbLaRkTTBadtWpgTN8NZLW0C1SEM" +
                        // 不加这个就不能分页
                        "&activityId=931B6E07-F6C8-4159-8E01-2EF8AC19DBBB" +
                        "&$top=48" +
                        "&cm=zh-cn" +
                        "&contentType=article,video,slideshow" +
                        "&responseSchema=cardview" +
                        // 不加这个就不是财经类新闻
                        "&query=finance_news" +
                        "&queryType=myfeed" );
                response = httpClient.execute(httpGet);

            }else{
                HttpGet httpGet = new HttpGet("https://assets.msn.cn/service/news/feed/pages/ntpxfeed" +
                        "?newsSkip=" + (pageNo * 48) +
                        "&InterestIds=" + tabId +
                        "&User=a-642E85B39C9B0A316B679A68FFFFFFFF" +
                        "&apikey=0QfOX3Vn51YCzitbLaRkTTBadtWpgTN8NZLW0C1SEM" +
                        "&cm=zh-cn" +
                        "&ocid=anaheim-ntp-feeds" +
                        "&wposchema=byregion" +
                        "&contentType=article,video,slideshow,webcontent");
                response = httpClient.execute(httpGet);
            }

            // 注意：获取某页时如果返回204，说明msn后台做了限制，只能返回这么多，暂时只能找到这个解释
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String jsonContent = EntityUtils.toString(entity, "UTF-8");
                return jsonContent;
            } else {
                log.info("response={}", response);
                log.error("获取Msn数据失败：Failed to fetch data with status code: {}", response.getStatusLine().getStatusCode());
            }

            response.close();
        } catch (IOException e) {
            log.error("获取Msn数据失败：{}", e.getMessage());
            return "error";
        }
        return "error";
    }

    /**
     * 获取新闻详情
     * @param newsId
     * @return
     */
    @GetMapping("/crud/getMsnDataDetail/{newsId}")
    public String getMsnDataDetail(@PathVariable String newsId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return "";
        }
        if (!StringUtils.hasLength(newsId)) {
            return "empty";
        }
        log.info("getMsnDataDetail tabId={}", newsId);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet("https://assets.msn.cn/content/view/v2/Detail/zh-cn/" + newsId);
            CloseableHttpResponse response = httpClient.execute(httpGet);

            // 注意：获取某页时如果返回204，说明msn后台做了限制，只能返回这么多，暂时只能找到这个解释
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "UTF-8");
            } else {
                log.info("response={}", response);
                log.error("获取Msn数据失败：Failed to fetch data with status code: {}", response.getStatusLine().getStatusCode());
            }

            response.close();
        } catch (IOException e) {
            log.error("获取Msn数据失败：{}", e.getMessage());
            return "error";
        }
        return "error";
    }
}
