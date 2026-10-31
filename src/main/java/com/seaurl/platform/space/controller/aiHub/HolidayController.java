package com.seaurl.platform.space.controller.aiHub;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.Holiday;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.service.HolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.List;

@RestController
@RequestMapping("/space")

public class HolidayController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    HolidayService holidayService;

    /**
     * 根据年份获取假期数据
     * @param year
     * @return
     */
    @GetMapping("/crud/holiday/{year}")
    public ResponseEntity<?> queryBy(@PathVariable String year) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(year)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            List<Holiday> result = holidayService.queryBy(year);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex){
            log.error("queryBy exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
