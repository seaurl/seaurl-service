package com.seaurl.platform.users.controller;

import com.alibaba.fastjson.JSON;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.users.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.users.request.UserLikeUrlRequest;
import com.seaurl.platform.users.request.UserPointsRequest;
import com.seaurl.platform.users.request.UserWatchRequest;
import com.seaurl.platform.users.service.AccountService;
import com.seaurl.platform.users.service.UrlService;
import com.seaurl.platform.users.service.UserLikeUrlService;
import com.seaurl.platform.common.notification.NotificationDowngradeService;
import com.seaurl.platform.users.service.facade.SpaceFacade;
import io.micrometer.observation.annotation.Observed;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/users")

@Observed
public class UserLikeUrlController {
    private Logger log = LoggerFactory.getLogger(this.getClass());


    @Autowired
    UserLikeUrlService userLikeUrlService;

//    @Autowired
//    AccountWebClient accountWebClient;

    @Autowired
    AccountService accountService;

    @Autowired
    SpaceFacade spaceFacade;

    @Autowired
    NotificationDowngradeService notificationDowngradeService;

    @Autowired
    UrlService urlService;


    /**
     * 根据用户id和网址列表查询用户点赞的网址列表
     *
     * @param userLikeUrlRe
     * @return
     */
    @PostMapping("/crud/like/url/queryByUserId")
    public ResponseEntity<?> queryLikeUrl(@RequestBody UserLikeUrlRequest userLikeUrlRe) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (userLikeUrlRe.getUrlIds().length <= 0) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            List<UserLikeUrl> userLikeUrlList = userLikeUrlService.queryLikeUrlByUserId(loginUser.getUid(), Arrays.asList(userLikeUrlRe.getUrlIds()));
            List<String> urlIds = new ArrayList<>();
            userLikeUrlList.forEach(item -> urlIds.add(item.getUrlId()));
            return ResponseEntity.ok(new ResultSuccess<>(urlIds));
        }catch (Exception ex) {
            log.error("queryLikeUrl exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 用户点赞链接url
     *
     * @param userLikeUrlRe
     * @return
     */
    @PostMapping("/crud/like/url")
    public ResponseEntity<?> addLikeUrl(@RequestBody UserLikeUrlRequest userLikeUrlRe) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null || !StringUtils.hasLength(userLikeUrlRe.getByUid())) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(userLikeUrlRe.getUrlId())) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            // 根据urlid查询网址信息
            Url theurl = urlService.queryById(userLikeUrlRe.getUrlId());
            log.info("the url ={}", theurl.getUid());

            UserLikeUrl queryUserLikeUrl = userLikeUrlService.isExist(loginUser.getUid(), userLikeUrlRe.getUrlId());
            Date date = new Date();
            UserLikeUrl userLikeUrl = new UserLikeUrl();
            userLikeUrl.setUrlId(userLikeUrlRe.getUrlId());
            userLikeUrl.setUid(loginUser.getUid());
            userLikeUrl.setUpdatedDt(date);
            Integer count = -1;
            if (queryUserLikeUrl != null) {
                // 点赞：如果valid=0,说明是没有点赞，所以要设置成=1
                if (queryUserLikeUrl.getValid() == 0) {
                    userLikeUrl.setValid(1);
                    count = 1;
                    // 添加点赞通知
                    addLikeUrlRemind(loginUser, userLikeUrlRe.getUrlId(), userLikeUrlRe.getByUid(), "赞了你的网址");
                    // 如果url网址有数据，并且不是自己点赞自己，则积分变动
                    if(theurl != null && !theurl.getUid().equals(loginUser.getUid())){
                        // 积分操作：add-url,copy-url,like-url,following-user
                        // delete-url,unlike-url,cancel-following-user
                    }
                }
                // 取消点赞：如果valid=1,说明是有点赞，所以要设置成=0
                if (queryUserLikeUrl.getValid() == 1) {
                    userLikeUrl.setValid(0);
                    count = 0;
                    // 删除点赞通知
                    deleteLikeUrlRemind(loginUser.getUid(), userLikeUrlRe.getByUid(), userLikeUrlRe.getUrlId());
                    // 如果url网址有数据，并且不是自己取消点赞自己，则积分变动
                    if(theurl != null && !theurl.getUid().equals(loginUser.getUid())){
                        // 积分操作：add-url,copy-url,like-url,following-user
                        // delete-url,unlike-url,cancel-following-user
                    }
                }
                userLikeUrlService.update(userLikeUrl);
            } else {
                // 没有点赞，则新增一条点赞数据
                count = 1;
                userLikeUrl.setValid(1);
                userLikeUrl.setCreatedDt(date);
                userLikeUrlService.add(userLikeUrl);
                // 添加点赞通知
            }
            return ResponseEntity.ok(new ResultSuccess<>(count));
        }catch (Exception ex) {
            log.error("addLikeUrl exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    private void deleteLikeUrlRemind(String uid, String byUid, String urlId) {
    }

    // 添加用户点赞网址给网址拥有者的通知提醒
    private void addLikeUrlRemind(LoginUser loginUser, String urlId, String byUserId, String action) {
        Mono<User> byUserMono = accountService.getUserByIdMono(byUserId);
        byUserMono.subscribe(byUser ->{
            Mono<Url> urlMono = spaceFacade.getUrlById(urlId);
            urlMono.subscribe(url ->{
                log.info("--------addLikeUrlRemind-----url res={}", JSON.toJSONString(url));
                if (byUser != null && url != null && byUser.getNotices().contains("likeMyUrlNotice")) {
                    if (!loginUser.getUid().equals(byUserId)) {
                        //            log.info("用户：{}允许别人复制自己的网址发送消息给自己", urlUser.getUserName());
                        NotifyRemind notifyRemind = new NotifyRemind();
                        ObjectId remindId = new ObjectId();
                        Date date = new Date();

                        notifyRemind.setRemindId(remindId.toString());
                        notifyRemind.setSenderId(loginUser.getUid());
//                        notifyRemind.setSenderName(loginUser.getUserName());
//                        notifyRemind.setSenderAvatar(loginUser.getAvatar());
                        notifyRemind.setSenderAction(action);
                        notifyRemind.setObjectId(url.getId());
                        notifyRemind.setObject(url.getTitle());
                        notifyRemind.setObjectType("url");
                        HashMap<String, String> urlmap = new HashMap<>();
                        urlmap.put("url", url.getUrl());
                        urlmap.put("icon", url.getFavicon());
                        notifyRemind.setObjectOthers(urlmap);
                        notifyRemind.setRecipientId(byUserId);
                        // 模板语法，如{张三}复制了你的网址 <a>淘宝</a>
                        notifyRemind.setMessage("{}" + action);
                        notifyRemind.setIsRead(false);
                        notifyRemind.setCreatedDt(date);
                        notifyRemind.setUpdatedDt(date);
                        // 这里使用mq来发送,在notify-service中接受消息
                        String sendInfo = JSON.toJSONString(notifyRemind);
                        log.info("sendInfo={}", sendInfo);
//                rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, RabbitMqConfig.ADD_USER_LIKE_URL_QUEUE, sendInfo);

                        // 添加复制网址提醒
//                ResponseEntity<?> sendResult = notifyFeignClient.addNotifyRemind(notifyRemind);
                    } else {
                        log.info("自己点赞自己网址不用发送消息");
                        //自己点赞自己网址不用发送消息
                    }
                } else {
//            log.info("用户：{}不允许别人复制自己的网址发送消息给自己", urlUser.getUserName());
                }
            });
        });
    }
}
