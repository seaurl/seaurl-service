package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.space.model.Hotline;
import com.seaurl.platform.common.model.Note;
import com.seaurl.platform.space.service.HotlineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.List;

@RestController
@RequestMapping("/space")

public class HotlineController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    HotlineService hotlineService;

    /**
     * 获取所有客户电话
     * @return
     */
    @GetMapping("/info/hotline/queryAll")
    public ResponseEntity<?> queryAll() {
        try {
            List<Hotline> hotlines = hotlineService.queryAll();
            return ResponseEntity.ok(new ResultSuccess<>(hotlines));
        }catch (Exception ex) {
            log.error("queryAll exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
