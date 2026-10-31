package com.seaurl.platform.space.controller;

import com.alibaba.fastjson.JSON;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.config.ServiceConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.request.FileRequest;
import com.seaurl.platform.space.request.UrlRequest;
import com.seaurl.platform.space.service.*;
import com.seaurl.platform.space.service.facade.UsersFacade;
import com.seaurl.platform.common.notification.NotificationDowngradeService;
import com.seaurl.platform.space.utils.DateUtils;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/space")

@Observed
public class UrlController {
    private static Logger log = LoggerFactory.getLogger(UrlController.class);

    @Autowired
    UrlCategoryService urlCategoryService;

    @Autowired
    UserService userService;

    @Autowired
    UrlService urlService;

    @Autowired
    ReptileService reptileService;

    @Autowired
    UserClickUrlService userClickUrlService;

    @Autowired
    UserCopyUrlService userCopyUrlService;

    @Autowired
    UsersFacade usersFacade;

    @Autowired
    NotificationDowngradeService notificationDowngradeService;

    @Autowired
    ServiceConfig serviceConfig;

    @Autowired
    FaviconService faviconService;


    /**
     * 根据urlid获取网址信息
     *
     * @param urlId
     * @return
     */
    @GetMapping("/info/url/getUrlById")
    public Url getUrlById(@RequestParam String urlId) {
        try {
            if (!StringUtils.hasLength(urlId)) {
                log.info("getUrlById urlid等于空");
                return null;
            }
            Url url = urlService.queryUrlById(urlId);
            log.info("getUrlById response ={}", JSON.toJSONString(url));
            return url;
        } catch (Exception ex) {
            log.error("getUrlById exception={}", ex.getMessage());
            return null;
        }
    }

    /**
     * Favicon 代理：解决浏览器跨域（CORP/CSP/防盗链）导致前端 {@code <img src=原站URL>} 加载失败。
     * <p>
     * 流程：查 Domain 表 → HTTP GET 原站 favicon 字节流 → 写 Redis 缓存（TTL 7 天）→ 返回。
     * 抓不到返回 302 重定向到默认图标。
     */
    @GetMapping("/info/favicon")
    public Mono<ResponseEntity<?>> getFavicon(@RequestParam String domain) {
        if (!StringUtils.hasLength(domain)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return Mono.fromCallable(() -> {
            byte[] bytes = faviconService.getFaviconBytes(domain);
            if (bytes == null || bytes.length == 0) {
                // 302 重定向到默认图标，前端 <img> 自动跟随
                return (ResponseEntity<?>) ResponseEntity.status(302)
                        .location(new java.net.URI("https://cdn.seaurl.com/space/url/www/default.png"))
                        .build();
            }
            return (ResponseEntity<?>) ResponseEntity.ok()
                    .contentType(guessImageMime(domain, bytes))
                    .cacheControl(org.springframework.http.CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS).cachePublic())
                    .body(bytes);
        }).onErrorResume(e -> {
            log.warn("getFavicon err domain={} msg={}", domain, e.getMessage());
            return Mono.just(ResponseEntity.status(302)
                    .location(java.net.URI.create("https://cdn.seaurl.com/space/url/www/default.png"))
                    .build());
        });
    }

    private static org.springframework.http.MediaType guessImageMime(String domain, byte[] bytes) {
        String d = domain == null ? "" : domain.toLowerCase();
        // baizhu.vip 等站用 svg，按内容首字节嗅探更可靠，但保持简单：默认 PNG，明显 svg 字节时改 SVG
        if (bytes.length > 4 && bytes[0] == '<' && (new String(bytes, 0, Math.min(bytes.length, 200), java.nio.charset.StandardCharsets.US_ASCII).toLowerCase().contains("<svg"))) {
            return org.springframework.http.MediaType.valueOf("image/svg+xml");
        }
        // 89 50 4E 47 = PNG magic
        if (bytes.length > 4 && (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50) {
            return org.springframework.http.MediaType.IMAGE_PNG;
        }
        // FF D8 FF = JPEG magic
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return org.springframework.http.MediaType.IMAGE_JPEG;
        }
        // GIF8
        if (bytes.length > 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return org.springframework.http.MediaType.IMAGE_GIF;
        }
        // 兜底用 ICO（favicon 最常见格式），浏览器宽容
        return org.springframework.http.MediaType.valueOf("image/x-icon");
    }


    /**
     * 设置是否收藏
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/setFavorite")
    public ResponseEntity<?> setFavorite(@RequestBody UrlRequest urlRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String urlId = urlRequest.getUrlId();
        boolean isFavorite = urlRequest.getIsFavorite();

        if (!StringUtils.hasLength(urlId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            boolean result = urlService.setFavorite(urlId, isFavorite);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.info("setFavorite ex={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 添加网址
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/add")
    public Mono<ResponseEntity<?>> addUrl(@RequestBody UrlRequest urlRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
        }
        String urlTitle = urlRequest.getUrlTitle();
        String cid = urlRequest.getCategoryId();
        String urlStr = urlRequest.getUrl();
        String description = urlRequest.getDescription();
        String keywords = urlRequest.getKeywords();

        if (!StringUtils.hasLength(urlTitle) || !StringUtils.hasLength(cid) || !StringUtils.hasLength(urlStr)) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
        try {
            Date current = new Date();
            Mono<User> userMono = usersFacade.getAccountInfoByUseId(loginUser.getUid());
            return userMono.flatMap(user -> {
                Url url = new Url();
                url.setUid(user.getUid());
                url.setUrl(urlStr);
                url.setTitle(urlTitle);
                url.setDescription(description);
                url.setKeywords(keywords);
                url.setCategoryId(cid);
                url.setIsCopy(false);
                url.setIsTop(false);
                url.setIsPrivate("0");
                url.setCreatedDt(current);
                url.setUpdatedDt(current);
                // 假设 addUrl 返回 Mono<Url>
                try {
                    return urlService.addUrl(url)
                            .map(result -> {
                                if (result != null && result.getId() != null) {
                                    return ResponseEntity.ok(new ResultSuccess<>(result));
                                } else {
                                    log.warn("addUrl returned null id for user {}", user.getUid());
                                    return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
                                }
                            });
                } catch (IOException e) {
                    log.warn("addUrl err {}", e.getMessage());
                    return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
                }
            });
        } catch (Exception ex) {
            log.error("addUrl exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 获取网址标题
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/getTitle")
    public ResponseEntity<?> getUrlTitle(@RequestBody UrlRequest urlRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        String url = urlRequest.getUrl();
        if (!StringUtils.hasLength(url)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            String title = reptileService.getUrlTitle(url);
            return ResponseEntity.ok(new ResultSuccess<>(title));
        } catch (Exception ex) {
            log.error("getUrlTitle exception ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/getTitleAndDesc")
    public ResponseEntity<?> getTitleAndDesc(@RequestBody UrlRequest urlRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        String url = urlRequest.getUrl();
        if (!StringUtils.hasLength(url)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            HashMap result = reptileService.getUrlTitleAndDesc(url);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getTitleAndDesc exception ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 复制网址
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/copy")
    public ResponseEntity<?> copyUrl(@RequestBody UrlRequest urlRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String urlId = urlRequest.getUrlId();
            String title = urlRequest.getUrlTitle();
            String categoryId = urlRequest.getCategoryId();
            String description = urlRequest.getDescription();
            String keywords = urlRequest.getKeywords();
            String isPrivate = urlRequest.getIsPrivate();
            if (!StringUtils.hasLength(urlId) || !StringUtils.hasLength(title) || !StringUtils.hasLength(categoryId) || !StringUtils.hasLength(isPrivate)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
//        User user = accountFeignClient.getAccountInfoByUserName(loginUser.getUserName());
            Url url = urlService.queryUrlById(urlId);

            if (url == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Url copyUrl = new Url();
            copyUrl.setUid(loginUser.getUid());
            copyUrl.setUrl(url.getUrl());
            copyUrl.setIsDelete(false);
            copyUrl.setFavicon(url.getFavicon());
            copyUrl.setDomain(url.getDomain());
            copyUrl.setTitle(title);
            copyUrl.setDescription(description);
            copyUrl.setKeywords(keywords);
            copyUrl.setCategoryId(categoryId);
            copyUrl.setIsTop(false);
            copyUrl.setIsCopy(true);
            copyUrl.setIsPrivate(isPrivate);
            copyUrl.setCreatedDt(new Date());
            copyUrl.setUpdatedDt(new Date());
            // 添加url
            Url resultAdd = urlService.copyUrl(copyUrl);
            if (resultAdd != null) {
                String newUrlId = resultAdd.getId();
                UserCopyUrl userCopyUrl = new UserCopyUrl();
                userCopyUrl.setFromUrlId(urlId);
                userCopyUrl.setToUrlId(newUrlId);
                userCopyUrl.setUid(loginUser.getUid());
                userCopyUrl.setCreatedDt(new Date());
                userCopyUrl.setUpdatedDt(new Date());
                // 添加copy
                boolean resultCopy = userCopyUrlService.addCopyUrl(userCopyUrl);

                return ResponseEntity.ok(new ResultSuccess<>(resultCopy));
            }
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.ADD_ERROR));
        } catch (Exception ex) {
            log.error("copyUrl exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 编辑网址
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/edit")
    public Mono<ResponseEntity<?>> editUrl(@RequestBody UrlRequest urlRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }
            String urlId = urlRequest.getUrlId();
            String title = urlRequest.getUrlTitle();
            String description = urlRequest.getDescription();
            String categoryId = urlRequest.getCategoryId();
            String keywords = urlRequest.getKeywords();
//            String isPrivate = urlRequest.getIsPrivate();
            if (!StringUtils.hasLength(urlId) || !StringUtils.hasLength(title) || !StringUtils.hasLength(categoryId)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }
            Url url = new Url();
            url.setId(urlId);
            url.setTitle(title);
            url.setDescription(description);
            url.setCategoryId(categoryId);
            url.setKeywords(keywords);
//            url.setIsPrivate(isPrivate);
            return urlService.editUrl(url)
                    .map(result -> {
                        return ResponseEntity.ok(new ResultSuccess<>(result));
                    });
        } catch (Exception ex) {
            log.error("editUrl exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 设置是否关注
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/updateAttention")
    public ResponseEntity<?> updateAttention(@RequestBody UrlRequest urlRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String urlId = urlRequest.getUrlId();
            boolean isAttention = urlRequest.getIsAttention();
            log.info("isAttention={}", isAttention);
            if (!StringUtils.hasLength(urlId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = urlService.updateAttention(urlId, isAttention);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("updateAttention exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除网址
     *
     * @param urlId
     * @return
     */
    @DeleteMapping("/crud/url/delete/{urlId}")
    public ResponseEntity<?> deleteUrl(@PathVariable String urlId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(urlId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            // todo:删除复制网址要把关联表也删除
            boolean result = urlService.deleteUrl(urlId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("deleteUrl exception ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据title模糊查询网址
     * todo:后面数据如果多要做分页
     *
     * @param urlRequest
     * @return
     */
//    @PostMapping("/info/url/queryUrlsByTitle")
//    public Mono<ResponseEntity<?>> queryUrlsByTitle(@RequestBody UrlRequest urlRequest) {
//        try {
//            String title = urlRequest.getUrlTitle();
//            String userName = urlRequest.getUserName();
//            if (!StringUtils.hasLength(title) || !StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                List<Url> urls = urlService.queryUrlByTitle(user.getUid(), title);
//                return ResponseEntity.ok(new ResultSuccess<>(urls));
//            });
//        }catch (Exception ex) {
//            log.error("queryUrlsByTitle exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }

    /**
     * @param urlRequest
     * @return
     */
    @PostMapping("/info/url/searchUrls")
    public Mono<ResponseEntity<?>> searchUrls(@RequestBody UrlRequest urlRequest) {
        try {
            String title = urlRequest.getUrlTitle();
            String userName = urlRequest.getUserName();
            String categoryId = urlRequest.getCategoryId();

            if (!StringUtils.hasLength(title) || !StringUtils.hasLength(userName)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }

            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
            return userMono.map(user -> {
                Integer pageNum = urlRequest.getPageNum();
                Integer pageSize = urlRequest.getPageSize();
                if (pageNum == null) {
                    pageNum = 1;
                }
                if (pageSize == null) {
                    pageSize = 60;
                }

                HashMap result;
                if (StringUtils.hasLength(categoryId)) {
                    result = urlService.queryUrlByTitle(categoryId, user.getUid(), title, pageNum, pageSize);
                } else {
                    result = urlService.queryUrlByTitle(user.getUid(), title, pageNum, pageSize);
                }
                return ResponseEntity.ok(new ResultSuccess<>(result));
            });
        } catch (Exception ex) {
            log.error("searchUrls exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 分页查询和搜索网址
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/queryAndSearchUrls")
    public ResponseEntity<?> queryAndSearchUrls(@RequestBody UrlRequest urlRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();

            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            String categoryId = urlRequest.getCategoryId();
            String search = urlRequest.getSearch();
            String sortType = urlRequest.getSortType();

            if (!StringUtils.hasLength(sortType)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            Integer pageNum = urlRequest.getPageNum();
            Integer pageSize = urlRequest.getPageSize();
            if (pageNum == null) {
                pageNum = 1;
            }
            if (pageSize == null) {
                pageSize = 60;
            }

            boolean isDate = DateUtils.isValidDate(search, "yyyy-MM-dd");
            boolean isMonth = DateUtils.isValidDate(search, "yyyy-MM");
            boolean isMyself = true;
            log.info("---------isMyself-------{}", isMyself);
            if (isDate) {
                QueryResults result = urlService.queryAndSearchUrls(loginUser.getUid(), categoryId, search, "%Y-%m-%d", sortType, isMyself, pageNum, pageSize);
                return ResponseEntity.ok(new ResultSuccess<>(result));
            }
            if (isMonth) {
                QueryResults result = urlService.queryAndSearchUrls(loginUser.getUid(), categoryId, search, "%Y-%m", sortType, isMyself, pageNum, pageSize);
                return ResponseEntity.ok(new ResultSuccess<>(result));
            }
            QueryResults result = urlService.queryAndSearchUrls(loginUser.getUid(), categoryId, search, sortType, isMyself, pageNum, pageSize);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("queryAndSearchUrls error", ex);
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 用户点击网址统计点击次数
     * 注意：只记录登录用户的点击
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/userClickUrl")
    public ResponseEntity<?> userClickUrl(@RequestBody UrlRequest urlRequest) {
        try {
            String urlId = urlRequest.getUrlId();
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(urlId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            UserClickUrl queryUserClickUrl = userClickUrlService.queryBy(loginUser.getUid(), urlId);

            if (queryUserClickUrl != null) {
                LocalDateTime currentDateTime = LocalDateTime.now();

                LocalDateTime updateDateTime = LocalDateTime.ofInstant(queryUserClickUrl.getUpdatedDt().toInstant(), ZoneId.systemDefault());
                long minutes = ChronoUnit.MINUTES.between(updateDateTime, currentDateTime);
                log.info("相差时间：{}", minutes);
                // 只有相差时间超过10分钟才会累积浏览数，防止恶意点击浏览
                if (minutes >= 10) {
                    int count = queryUserClickUrl.getClickCount();
                    boolean result = userClickUrlService.updateClickCount(queryUserClickUrl.getId(), count + 1);
                    return ResponseEntity.ok(new ResultSuccess<>(result));
                }
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Unknown_Exception));
            } else {
                UserClickUrl userClickUrl = new UserClickUrl();
                userClickUrl.setUid(loginUser.getUid());
                userClickUrl.setUrlId(urlId);
                userClickUrl.setClickCount(1);
                userClickUrl.setCreatedDt(new Date());
                userClickUrl.setUpdatedDt(new Date());
                userClickUrlService.add(userClickUrl);
                return ResponseEntity.ok(new ResultSuccess<>(true));
            }
        } catch (Exception ex) {
            log.error("userClickUrl exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取最新用户网址动态
     *
     * @param userName
     * @return
     */
//    @GetMapping("/info/url/{userName}/getLatestNewsForUrls")
//    public Mono<ResponseEntity<?>> getLatestNewsForUrls(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                Map<String, Object> map = urlService.getUrlsForLatestMonth(user.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(map));
//            });
//        }catch (Exception ex) {
//            log.error("getLatestNewsForUrls exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }

    /**
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/getUrlsByFlag")
    public Mono<ResponseEntity<?>> getUrlsByFlag(@RequestBody UrlRequest urlRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null || !StringUtils.hasLength(loginUser.getUserName())) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }

            String flag = urlRequest.getFlag();
            if (!StringUtils.hasLength(flag)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }

            Mono<User> userMono = usersFacade.getAccountInfoByUserName(loginUser.getUserName());
            return userMono.map(user -> {
                List<HashMap> map = urlService.getUrlsForLatestMonth(user.getUid(), flag, true);
                return ResponseEntity.ok(new ResultSuccess<>(map));
            });
        } catch (Exception ex) {
            log.error("getUrlsByFlag exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 统计用户复制的url列表数据
     *
     * @param userName
     * @return
     */
//    @GetMapping("/info/url/{userName}/getUrlCopyList")
//    public Mono<ResponseEntity<?>> getUrlCopyList(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                List<HashMap> map = userCopyUrlService.getUrlCopyList(user.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(map));
//            });
//        }catch (Exception ex) {
//            log.error("getUrlCopyList exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }

    /**
     * 获取用户常用网址
     *
     * @param userName
     * @return
     */
    @GetMapping("/crud/url/getUserMostClickUrlList")
    public ResponseEntity<?> getUserMostClickUrlList() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            // crud肯定是自己了
            List<HashMap> result = urlService.getUserClickUrls(loginUser.getUid(), true);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getUserMostClickUrlList exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取用户访问的历史记录网站
     *
     * @return
     */
    @GetMapping("/crud/url/getUserHistoryUrlList")
    public ResponseEntity<?> getUserHistoryUrlList() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            // crud说明是自己
            List<HashMap> result = urlService.getUserHistoryUrls(loginUser.getUid(), true);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getUserMostClickUrlList exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/url/getUrlCount")
    public ResponseEntity<?> getUrlCountByType() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();

            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            //frequently
            Object r1 = urlService.getUserClickUrlsCount(loginUser.getUid(), true);

            // history
            Object r2 = urlService.getUserHistoryUrlsCount(loginUser.getUid(), true);

            // favorite
            Object r3 = urlService.queryIsFavoriteUrlCount(loginUser.getUid(), true);

            // top
            Object r4 = urlService.queryIsTopUrlCount(loginUser.getUid(), true);

            HashMap<String, Object> result = new HashMap<>();
            result.put("frequently", r1);
            result.put("history", r2);
            result.put("favorite", r3);
            result.put("top", r4);

            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.error("getUserMostClickUrlList exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param type
     * @return
     */
    @GetMapping("/crud/url/getUrlListByType")
    public ResponseEntity<?> getUrlListByType(@RequestParam String type) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();

            if (loginUser == null || !StringUtils.hasLength(type)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            List<HashMap> result1 = null;
            List<Url> result2 = null;
            boolean isMyself = true;

            if (type.equals("frequently")) {
                result1 = urlService.getUserClickUrls(loginUser.getUid(), isMyself);
                return ResponseEntity.ok(new ResultSuccess<>(result1));
            }
            if (type.equals("history")) {
                result1 = urlService.getUserHistoryUrls(loginUser.getUid(), isMyself);
                return ResponseEntity.ok(new ResultSuccess<>(result1));
            }
            if (type.equals("favorite")) {
                result2 = urlService.queryIsFavoriteUrlList(loginUser.getUid(), isMyself);
                return ResponseEntity.ok(new ResultSuccess<>(result2));
            }
            if (type.equals("top")) {
                result2 = urlService.queryIsTopUrlList(loginUser.getUid(), isMyself);
                return ResponseEntity.ok(new ResultSuccess<>(result2));
            }
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));

        } catch (Exception ex) {
            log.error("getUserMostClickUrlList exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 获取用户关注的网址列表
     *
     * @param userName
     * @return
     */
//    @GetMapping("/info/url/{userName}/getAttentionUrl")
//    public Mono<ResponseEntity<?>> getAttentionUrl(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                List<Url> urls = urlService.getAttentionUrl(user.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(urls));
//            });
//        }catch (Exception ex) {
//            log.error("getAttentionUrl exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }

    /**
     * 获取快速复制网址列表
     *
     * @param userName
     * @return
     */
//    @GetMapping("/info/url/{userName}/getFastCopyUrl")
//    public Mono<ResponseEntity<?>> getFastCopyUrl(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                List<Url> urls = urlService.getFastCopyUrl(user.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(urls));
//            });
//        }catch (Exception ex) {
//            log.error("getFastCopyUrl exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }

    /**
     * 获取用户收藏的网址列表
     *
     * @param userName
     * @return
     */
//    @GetMapping("/info/url/{userName}/getCollectionUrl")
//    public Mono<ResponseEntity<?>> getCollectionUrl(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                List<Url> urls = urlService.getCollectionUrl(user.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(urls));
//            });
//        }catch (Exception ex) {
//            log.error("getCollectionUrl exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }

    /**
     * 查询用户添加网址最多前10
     *
     * @param userName
     * @return
     */
//    @GetMapping("/info/url/{userName}/getMostUrlsRank")
//    public Mono<ResponseEntity<?>> getMostUrlsRank(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                List<MostUrlsRank> urls = urlService.mostUrlsRank(user.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(urls));
//            });
//        }catch (Exception ex) {
//            log.error("getMostUrlsRank exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }

    /**
     * 设置置顶
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/url/top")
    public ResponseEntity<?> updateTop(@RequestBody UrlRequest urlRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(urlRequest.getUrlId())) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = urlService.setTop(urlRequest.getUrlId(), urlRequest.getIsTop());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("updateTop exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
