package com.seaurl.platform.users.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.users.model.Level;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.model.UserPoints;
import com.seaurl.platform.users.request.UserPointsRequest;
import com.seaurl.platform.users.service.LevelService;
import com.seaurl.platform.users.service.UserPointsService;
import com.seaurl.platform.users.service.UserService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/users")

@Observed
public class UserPointsController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    UserPointsService userPointsService;

    @Autowired
    LevelService levelService;

    @Autowired
    UserService userService;


    /**
     * 获取用户积分和等级
     * @param userPointsRequest
     * @return
     */
    @GetMapping("/info/user/getPoints")
    public ResponseEntity<?> getPoints(@RequestBody UserPointsRequest userPointsRequest) {
        try {
            String userName = userPointsRequest.getUserName();
            if (!StringUtils.hasLength(userName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            User user = userService.getUserByName(userName);
            Level level = levelService.getLevelByUid(user.getUid());

            return ResponseEntity.ok(new ResultSuccess<>(level));
        }catch (Exception ex) {
            logger.error("getPoints exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
