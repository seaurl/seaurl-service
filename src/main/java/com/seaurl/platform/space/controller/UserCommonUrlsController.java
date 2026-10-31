package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.model.UserCommonUrls;
import com.seaurl.platform.space.model.UserTools;
import com.seaurl.platform.space.request.UserCommonUrlsRequest;
import com.seaurl.platform.space.request.UserToolsRequest;
import com.seaurl.platform.space.service.CommonUrlsGroupService;
import com.seaurl.platform.space.service.CommonUrlsService;
import com.seaurl.platform.space.service.UserCommonUrlsService;
import com.seaurl.platform.space.service.UserService;
import com.seaurl.platform.space.service.facade.UsersFacade;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/space")

@Observed
public class UserCommonUrlsController {
    private static Logger log = LoggerFactory.getLogger(UserCommonUrlsController.class);

    @Autowired
    CommonUrlsService commonUrlsService;


    @Autowired
    CommonUrlsGroupService commonUrlsGroupService;


    @Autowired
    UserCommonUrlsService userCommonUrlsService;

    @Autowired
    UserService userService;

    @Autowired
    UsersFacade usersFacade;

    @PostMapping("/crud/userCommonUrls/addOrRemove")
    public ResponseEntity<?> addUserTools(@RequestBody UserCommonUrlsRequest userCommonUrlsRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String groupId = userCommonUrlsRequest.getGroupId();
            if (!StringUtils.hasLength(groupId) ) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            UserCommonUrls queryUserCommonUrl = userCommonUrlsService.queryBy(loginUser.getUid(), groupId);
            // 判断是否已存在，不存在就添加 ，否则 就删除
            if (queryUserCommonUrl == null) {
                Date current = new Date();
                UserCommonUrls userTools = new UserCommonUrls();
                userTools.setUid(loginUser.getUid());
                userTools.setGroupId(groupId);
                userTools.setCreatedDt(current);
                userTools.setUpdatedDt(current);

                boolean result = userCommonUrlsService.add(userTools);
                return ResponseEntity.ok(new ResultSuccess<>(result));
            } else {
                boolean result = userCommonUrlsService.remove(loginUser.getUid(), groupId);
                return ResponseEntity.ok(new ResultSuccess<>(result));
            }
        }catch (Exception ex) {
            log.error("addUserTools exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取用户常用网址(单表查询)
     * @param userName
     * @return
     */
    @GetMapping("/info/{userName}/userCommonUrls")
    public Mono<ResponseEntity<?>> getUserCommonUrls(@PathVariable String userName) {
        try {
            if (!StringUtils.hasLength(userName)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }

            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
            return userMono.map(user -> {
                List<UserCommonUrls> userTools = userCommonUrlsService.queryByUid(user.getUid());
                return ResponseEntity.ok(new ResultSuccess<>(userTools));
            });
        }catch (Exception ex) {
            log.error("getUserCommonUrls exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     *获取用户常用网址(多表查询)
     * @param userName
     * @return
     */
    @GetMapping("/info/{userName}/fullUserCommonUrls")
    public Mono<ResponseEntity<?>> getFullUserCommonUrls(@PathVariable String userName) {
        try {
            if (!StringUtils.hasLength(userName)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }

            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
            return userMono.map(user -> {
                List<HashMap> userTools = userCommonUrlsService.queryUserCommonUrls(user.getUid());
                return ResponseEntity.ok(new ResultSuccess<>(userTools));
            });
        }catch (Exception ex) {
            log.error("getFullUserCommonUrls exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }
}
