package com.seaurl.platform.users.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.users.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.users.request.LatestNewsRequest;
import com.seaurl.platform.users.service.AccountService;
import com.seaurl.platform.users.service.UserEventService;
import com.seaurl.platform.users.service.UserFollowService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/users")

@Observed
public class UserEventController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

//    @Autowired
//    AccountWebClient accountWebClient;

    @Autowired
    AccountService accountService;

    @Autowired
    UserEventService userEventService;

    @Autowired
    UserFollowService userFollowService;

    /**
     * 添加用户动态
     *
     * @return
     */
//    @PostMapping("/inner/userEvent/add")
//    public ResponseEntity<?> add(@RequestBody UserEvent userEvent) {
//        log.info("add user event={}", userEvent.toString());
//        userEventService.add(userEvent);
//        return ResponseEntity.ok(new ResultSuccess<>(true));
//    }


    /**
     * 删除用户动态
     *
     * @param id
     * @return
     */
    @DeleteMapping("/crud/userEvent/delete/{id}")
    public ResponseEntity<?> deleteUserEvent(@PathVariable String id) {
        try {
            if (!StringUtils.hasLength(id)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = userEventService.remove(id);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("deleteUserEvent exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取用户动态
     *
     * @param latestNewsRequest
     * @param response
     * @return
     */
    @PostMapping("/crud/getUserEvents")
    public ResponseEntity<?> getLatestNewsForUsers(@RequestBody LatestNewsRequest latestNewsRequest, ServerHttpResponse response) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        Integer pageSize = latestNewsRequest.getPageSize();
        Integer pageNum = latestNewsRequest.getPageNum();
        String type = latestNewsRequest.getType();
        if (pageNum == null || pageSize == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        if (!StringUtils.hasLength(type)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            List<UserFollow> userFollows = userFollowService.getMyFollowUserList(loginUser.getUid());
            List<String> userIds = new ArrayList<>();
            userIds.add(loginUser.getUid());
            if (userFollows.size() > 0) {
                userFollows.forEach(item -> userIds.add(item.getFollowedId()));
            }
            // 为你推荐(本周)
            if (type.equals("1")) {
                PageResults results = userEventService.getUserEvents(loginUser.getUid(), pageNum, pageSize);
                return ResponseEntity.ok(new ResultSuccess<>(results));
            }
            // 正在关注
            if (type.equals("2")) {
                PageResults results = userEventService.getUserEvents(userIds, pageNum, pageSize);
                return ResponseEntity.ok(new ResultSuccess<>(results));
            }
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        } catch (Exception ex) {
            log.error("getLatestNewsForUsers error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Unknown_Exception));
        }
    }
}
