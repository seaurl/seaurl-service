package com.seaurl.platform.users.controller;

import com.alibaba.fastjson.JSON;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.users.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.users.request.UserFollowRequest;
import com.seaurl.platform.users.request.UserPointsRequest;
import com.seaurl.platform.users.service.UserFollowService;
import com.seaurl.platform.users.service.UserService;
import com.seaurl.platform.common.notification.NotificationDowngradeService;
import io.micrometer.observation.annotation.Observed;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import reactor.core.publisher.Mono;

import java.util.Date;

@RestController("usersUserFollowController")
@RequestMapping("/users")

@Observed
public class UserFollowController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    UserService userService;

    @Autowired
    UserFollowService userFollowService;

    @Autowired
    NotificationDowngradeService notificationDowngradeService;

//    @Autowired
//    RabbitTemplate rabbitTemplate;


    /**
     *
     * @param userFollowRequest
     * @return
     */
    @PostMapping("/crud/user/isFollowed")
    public ResponseEntity<?> isFollowed(@RequestBody UserFollowRequest userFollowRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
//        String userName = userFollowRequest.getUserName();
            String followedUseName = userFollowRequest.getFollowedUserName();
            if (!StringUtils.hasLength(followedUseName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
//        User me = userService.getUserByName(userName);
            User others = userService.getUserByName(followedUseName);
            if (others == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = userFollowService.isFollowed(loginUser.getUid(), others.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("isFollowed exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 关注或取消关注
     *
     * @param userFollowRequest
     * @return
     */
    @PostMapping("/crud/user/followUser")
    public ResponseEntity<?> followUser(@RequestBody UserFollowRequest userFollowRequest) {
        try {
            log.info("-----followUser start-----");
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            // 被关注者或被取消者用户名
            String followedUseName = userFollowRequest.getFollowedUserName();
            if (!StringUtils.hasLength(followedUseName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            User others = userService.getUserByName(followedUseName);
            if (others == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Date date = new Date();
            UserFollow userFollow = new UserFollow();
            userFollow.setFollowedId(others.getUid());
            userFollow.setUid(loginUser.getUid());
            userFollow.setStatus(userFollowRequest.getStatus() != null && userFollowRequest.getStatus() ? 1 : 0);
            userFollow.setCreatedDt(date);
            userFollow.setUpdatedDt(date);
            boolean result = userFollowService.follwedUser(userFollow);
            log.info("关注状态={}", userFollowRequest.getStatus());
            if (result && userFollowRequest.getStatus()) {
                log.info("关注用户开始提醒了...");
                followedUserRemind(loginUser, others, "关注了我");
            }
            if(result && userFollowRequest.getStatus()){
                // 积分操作：add-url,copy-url,like-url,following-user
                // delete-url,unlike-url,cancel-following-user
            }
            if(result && !userFollowRequest.getStatus()){
                // 积分操作：add-url,copy-url,like-url,following-user
                // delete-url,unlike-url,cancel-following-user
            }
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("followUser exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 关注用户提醒
     *
     * @param user
     * @param others
     * @param action
     */
    private void followedUserRemind(LoginUser user, User others, String action) {
        if (others.getNotices().contains("followingMeNotice")) {
            log.info("用户：{}允许别人关注自己时发送消息给自己", others.getUserName());
            NotifyRemind notifyRemind = new NotifyRemind();
            ObjectId id = new ObjectId();
            ObjectId remindId = new ObjectId();
            Date date = new Date();

            notifyRemind.setId(id.toString());
            notifyRemind.setRemindId(remindId.toString());
            notifyRemind.setSenderId(user.getUid());
//            notifyRemind.setSenderName(user.getUserName());
//            notifyRemind.setSenderAvatar(user.getAvatar());
            notifyRemind.setSenderAction(action);
            notifyRemind.setObjectId(others.getUid());
            notifyRemind.setObject(others.getUserName());
            notifyRemind.setObjectType("user");
            notifyRemind.setRecipientId(others.getUid());
            notifyRemind.setMessage("{}关注了我");
            notifyRemind.setIsRead(false);
            notifyRemind.setCreatedDt(date);
            notifyRemind.setUpdatedDt(date);
            // 添加复制网址提醒
            // todo:用mq发送?
            Mono<Boolean> sendResult = notificationDowngradeService.addNotifyRemind(notifyRemind);
            sendResult.subscribe(responseBody -> {
                // 处理响应结果
                log.info("--- Response: {}" ,responseBody);
                if (responseBody){
                    log.info("--- 发送成功");
                }else{
                    log.info("--- 发送失败");
                }
            });
            log.info("已经发送关注消息了，有没有收到我就不知道了！");
        } else {
            log.info("用户：{}不允许别人关注自己时发送消息给自己", others.getUserName());
        }
    }
}
