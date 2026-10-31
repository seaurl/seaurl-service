package com.seaurl.platform.users.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.PageResults;
import com.seaurl.platform.users.request.PaginationRequest;
import com.seaurl.platform.users.service.UserService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/users")

@Observed
public class RecommendUserController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    UserService userService;


    /**
     * 获取用户喜欢的网址top10
     *
     * @return
     */
    @GetMapping("/crud/user/getTop10UsersInMonth")
    public ResponseEntity<?> getTop10UsersInMonth() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            List<String> uids = userService.getFollowingUserIdList(loginUser.getUid());
            // 也不包含我自己
            uids.add(loginUser.getUid());
            List<HashMap> result = userService.getTop10UsersInMonth(uids);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            logger.error("getTop10UsersInMonth exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据分页获取用户列表
     *
     * @return
     */
    @PostMapping("/crud/user/getTopUsersByPage")
    public ResponseEntity<?> getTopUsersByPage(@RequestBody PaginationRequest paginationRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            Integer pageNum = paginationRequest.getPageNum();
            Integer pageSize = paginationRequest.getPageSize();
            if (pageNum == null) {
                pageNum = 1;
            }
            if (pageSize == null) {
                pageSize = 20;
            }
            List<String> uids = userService.getFollowingUserIdList(loginUser.getUid());
            // 也不包含我自己
            uids.add(loginUser.getUid());
            PageResults result = userService.getTopUsersByPage(uids, pageNum, pageSize);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            logger.error("getTopUsersByPage exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
