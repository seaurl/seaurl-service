package com.seaurl.platform.users.controller;

import com.alibaba.fastjson.JSONArray;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.users.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.users.request.*;
import com.seaurl.platform.users.service.*;
import com.seaurl.platform.users.service.facade.SpaceFacade;
import com.seaurl.platform.users.utils.IpUtils;
import io.micrometer.observation.annotation.Observed;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")

@Observed
public class UsersController {
    private static Logger log = LoggerFactory.getLogger(UsersController.class);

    @Autowired
    UserService userService;

    @Autowired
    UserWatchService userWatchService;

    @Autowired
    UrlCategoryService urlCategoryService;

    @Autowired
    UrlService urlService;

    @Autowired
    UserPinsService userPinsService;

    @Autowired
    UserFollowService userFollowService;

    @Autowired
    SpaceFacade spaceFacade;

    //转换文件流类型
    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    // 搜索ip地址
    public String searchIpAddr(ServerHttpRequest request) {
        String ip = IpUtils.getIpAddr(request);
        log.info("---------ip={}", ip);
        Searcher searcher = null;
        try {
            InputStream fileStream = new ClassPathResource("ipdb/ip2region.xdb").getInputStream();
            searcher = Searcher.newWithBuffer(toByteArray(fileStream));
        } catch (IOException e) {
            return "";
        }
        try {
            String region = searcher.search(ip);
            searcher.close();
            return region;

        } catch (Exception e) {
        }
        return "";
    }

    /**
     * @return
     */
    @PostMapping("/crud/getMyFriends")
    public Mono<ResponseEntity<?>> getMyFriends(@RequestBody FileShareRequest fileShareRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
        }
        String userName = fileShareRequest.getUserName();
        String fileId = fileShareRequest.getFileId();
        log.info("userName={}", userName);
        log.info("fileId={}", fileId);

        if (!StringUtils.hasLength(userName) || !StringUtils.hasLength(fileId)) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
        try {
            // 获取该文件所有被分享的用户
            Mono<File> fs = spaceFacade.getSharedFriends(fileId, loginUser);
            return fs.switchIfEmpty(Mono.defer(() -> {
                        // 返回一个空的 FileShare 对象，以保持类型一致
                        File emptyFileShare = new File();  // 或者根据你的需求设置合适的默认值
                        return Mono.just(emptyFileShare);  // 返回类型为 Mono<FileShare>
                    })
            ).flatMap(res -> {
                log.info("res = {}", JSONArray.toJSONString(res));
                List<String> uids = new ArrayList<>();
                List<User> user;
                uids.add(loginUser.getUid());
                if (res == null || res.getShareToFriends() == null || res.getShareToFriends().isEmpty()) {
                    user = userService.getUserByName(uids, userName, 5);
                    return Mono.just(ResponseEntity.ok(new ResultSuccess<>(user)));
                } else {
                    List<String> fids = res.getShareToFriends().stream()
                            .map(User::getUid)
                            .toList();
                    uids.addAll(fids);
                    user = userService.getUserByName(uids, userName, 5);
                }
                return Mono.just(ResponseEntity.ok(new ResultSuccess<>(user)));
            });
        } catch (Exception ex) {
            log.error("getUserInfo Exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 根据用户名获取用户信息
     *
     * @param username
     * @return
     */
    @GetMapping("/info/{username}")
    public ResponseEntity<?> getUserInfo(@PathVariable String username, ServerHttpRequest request) {
//        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (!StringUtils.hasLength(username)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            User user = userService.getUserByName(username);
            return ResponseEntity.ok(new ResultSuccess<>(user));
        } catch (Exception ex) {
            log.error("getUserInfo Exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 记录看了谁
     * 不能记录看自己
     *
     * @param userWatchRequest
     * @return
     */
    @PostMapping("/info/watch")
    public ResponseEntity<?> watchUser(@RequestBody UserWatchRequest userWatchRequest) {
        try {
            String userName = userWatchRequest.getUserName();
            String watchUserName = userWatchRequest.getWatchUserName();

            if (!StringUtils.hasLength(userName) || !StringUtils.hasLength(watchUserName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            if (userName.equals(watchUserName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
            }
            User user = userService.getUserByName(userName);
            User watchUser = userService.getUserByName(watchUserName);

            UserWatch userWatch = new UserWatch();
            userWatch.setUid(user.getUid());
            userWatch.setWatchUid(watchUser.getUid());
            userWatch.setCreatedDt(new Date());
            userWatch.setUpdatedDt(new Date());
            userWatchService.add(userWatch);
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.error("watchUser exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取用户访问的用户数据
     *
     * @param userName
     * @return
     */
    @GetMapping("/info/{userName}/getWatchUserToToday")
    public ResponseEntity<?> getWatchUserToToday(@PathVariable String userName) {
        try {
            if (!StringUtils.hasLength(userName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            User user = userService.getUserByName(userName);
            List<HashMap> users = userWatchService.getWatchUserToTodayList(user.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(users));
        } catch (Exception ex) {
            log.error("getWatchUserToToday exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取用户统计数据
     *
     * @param
     * @return
     */
    @GetMapping("/crud/data")
    public ResponseEntity<?> getUserData() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            Map<String, Object> userData = userService.getUserData(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(userData));
        } catch (Exception ex) {
            log.error("getUserData exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取我关注的
     *
     * @param userFollowPageRequest
     * @return
     */
    @PostMapping("/info/getFollowList")
    public ResponseEntity<?> getFollowList(@RequestBody UserFollowPageRequest userFollowPageRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            String username = userFollowPageRequest.getUserName();
//        String loginUserName = userFollowPageRequest.getLoginUserName();
            Long pageSize = userFollowPageRequest.getPageSize();
            Long pageNum = userFollowPageRequest.getPageNum();
            String type = userFollowPageRequest.getType();
            if (!StringUtils.hasLength(username) || pageNum == null || pageSize == null || !StringUtils.hasLength(type)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            List<HashMap> followList = new ArrayList<>();
            // 查询用户信息
            User user = userService.getUserByName(username);
//        User loginUser = null;
//        if (loginUser!=null) {
//            // 查询登录的用户信息
//            loginUser = userService.getUserByName(loginUserName);
//        }

            // 除了获取关注我的人的数据，还要获取我关注的人的id列表，目的是为了方便前端判断我是否关注了某人
            // 查询登录的用户关注的所有uid列表
            List<String> uids = new ArrayList<>();
            if (loginUser != null) {
                uids = userService.getFollowingUserIdList(loginUser.getUid());
            }
            // 我关注的
            if (type.equals("following")) {
                followList = userService.getFollowingList(user.getUid(), pageNum, pageSize);
            }
            // 关注我的
            if (type.equals("follower")) {
                followList = userService.getFollowerList(user.getUid(), pageNum, pageSize);
            }
            HashMap<String, Object> data = new HashMap<>();
            data.put("followList", followList);
            data.put("uids", uids);
            return ResponseEntity.ok(new ResultSuccess<>(data));
        } catch (Exception ex) {
            log.error("getFollowList exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据用户名查询网址分类
     *
     * @param userName
     * @return
     */
    @GetMapping("/info/{userName}/categoryTreeList")
    public ResponseEntity<?> queryCategoryTreeList(@PathVariable String userName) {
        try {
            if (!StringUtils.hasLength(userName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            User user = userService.getUserByName(userName);
            List<UrlCategory> urlCategoryList = urlCategoryService.queryCategoryByUid(user.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(urlCategoryList));
        } catch (Exception ex) {
            log.error("queryCategoryTreeList exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据分类id查询网址
     *
     * @param cid
     * @return
     */
    @GetMapping("/info/getUrlBy/{cid}")
    public ResponseEntity<?> getUrlByCategoryId(@PathVariable String cid) {
        try {
            if (!StringUtils.hasLength(cid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            List<Url> urls = urlService.queryUrlByCategoryId(cid);
            return ResponseEntity.ok(new ResultSuccess<>(urls));
        } catch (Exception ex) {
            log.error("getUrlByCategoryId exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param userName
     * @return
     */
    @GetMapping("/info/{userName}/getAllUrl")
    public ResponseEntity<?> getAllUrl(@PathVariable String userName) {
        try {
            if (!StringUtils.hasLength(userName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            User user = userService.getUserByName(userName);
            List<Url> urls = urlService.getAllUrlsByUid(user.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(urls));
        } catch (Exception ex) {
            log.error("getAllUrl exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据网址名称查询网址
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/info/queryUrlsByTitle")
    public ResponseEntity<?> queryUrlsByTitle(@RequestBody UrlRequest urlRequest) {
        try {
            String title = urlRequest.getUrlTitle();
            String userName = urlRequest.getUserName();
            if (!StringUtils.hasLength(title) || !StringUtils.hasLength(userName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            User user = userService.getUserByName(userName);
            List<Url> urls = urlService.queryUrlByTitle(user.getUid(), title);
            return ResponseEntity.ok(new ResultSuccess<>(urls));
        } catch (Exception ex) {
            log.error("queryUrlsByTitle exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据用户名获取pins
     *
     * @param userName
     * @return
     */
    @GetMapping("/info/{userName}/getPins")
    public ResponseEntity<?> getPins(@PathVariable String userName) {
        try {
            log.info("getPins username={}", userName);
            if (!StringUtils.hasLength(userName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            User user = userService.getUserByName(userName);
            UserPins userPins = userPinsService.queryByUid(user.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(userPins));
        } catch (Exception ex) {
            log.error("getPins exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 设置pins
     *
     * @param userPinsRequest
     * @return
     */
    @PostMapping("/info/setPins")
    public ResponseEntity<?> setPins(@RequestBody UserPinsRequest userPinsRequest) {
        try {
            String userName = userPinsRequest.getUserName();
            String urlId = userPinsRequest.getUrlIds();

            if (!StringUtils.hasLength(userName) || !StringUtils.hasLength(urlId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            String[] urlIds = urlId.split(",");
            User user = userService.getUserByName(userName);
            UserPins userPins = userPinsService.queryByUid(user.getUid());
            List<Url> urlList = urlService.queryByIds(urlIds);
            if (userPins != null) {
                userPins.setUrlList(urlList);
                userPinsService.update(userPins);
            } else {
                UserPins newUserPins = new UserPins();
                newUserPins.setUid(user.getUid());
                newUserPins.setUrlList(urlList);
                newUserPins.setCreatedDt(new Date());
                newUserPins.setUpdatedDt(new Date());
                userPinsService.add(newUserPins);
            }
            return ResponseEntity.ok(new ResultSuccess<>(urlList));
        } catch (Exception ex) {
            log.error("setPins exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 推送给用户关注的用户列表
     *
     * @param userName
     * @return
     */
    @GetMapping("/info/{userName}/getRecommendedAttentionUsers")
    public ResponseEntity<?> getRecommendedAttentionUsers(@PathVariable String userName) {
        return null;
    }

    /**
     * 获取好友最新动态
     *
     * @param latestNewsRequest
     * @return
     */
    @PostMapping("/crud/getLatestNewsForUsers")
    public ResponseEntity<?> getLatestNewsForUsers(@RequestBody LatestNewsRequest latestNewsRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            Integer pageSize = latestNewsRequest.getPageSize();
            Integer pageNum = latestNewsRequest.getPageNum();
            if (pageNum == null || pageSize == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            List<UserFollow> userFollows = userFollowService.getMyFollowUserList(loginUser.getUid());
            List<String> userIds = new ArrayList<>();
            userIds.add(loginUser.getUid());
            userFollows.forEach(item -> userIds.add(item.getFollowedId()));
            List<HashMap> hashMaps = urlService.getMyFollowingUrlList(userIds, pageNum, pageSize);
            return ResponseEntity.ok(new ResultSuccess<>(hashMaps));
        } catch (Exception ex) {
            log.error("getLatestNewsForUsers exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 更新用户首页背景图片
     *
     * @param userRequest
     * @return
     */
    @PostMapping("/info/updateUserBackgroundImg")
    public ResponseEntity<?> updateUserBackgroundImg(@RequestBody UserRequest userRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String imgSrc = userRequest.getImgSrc();
            if (!StringUtils.hasLength(imgSrc)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = userService.updateUserBackgroundImg(loginUser.getUid(), imgSrc);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("updateUserBackgroundImg exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
