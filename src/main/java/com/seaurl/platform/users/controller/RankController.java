package com.seaurl.platform.users.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.model.User;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")

@Observed
public class RankController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    UserPointsService userPointsService;

    @Autowired
    LevelService levelService;

    @Autowired
    UserService userService;

    /**
     * 查询用户排行
     *
     * @param param
     * @return
     */
    @GetMapping("/info/rank/getRankBy/{param}")
    public ResponseEntity<?> getUserLevelBy(@PathVariable String param) {
        try {
            if (!StringUtils.hasLength(param)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            String[] paramList = {"today", "week", "month", "year", "all"};
            boolean contains = Arrays.toString(paramList).contains(param);
            if (!contains){
                // todo
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Unknown_Exception));
            }

            List<Map> result;
            if (param.equals("all")){
                result = levelService.getAllRankList();
            }else{
                result = levelService.getRankListBy(param);
            }
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            logger.error("getMailbox exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
