package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.space.model.CommonUrlsGroup;
import com.seaurl.platform.space.service.CommonUrlsGroupService;
import com.seaurl.platform.space.service.CommonUrlsService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.List;

@RestController
@RequestMapping("/space")

@Observed
public class CommonUrlsController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CommonUrlsService commonUrlsService;


    @Autowired
    CommonUrlsGroupService commonUrlsGroupService;

    @GetMapping("/info/getCommonUrls")
    public ResponseEntity<?> getCommonUrls() {
        try {
            List<CommonUrlsGroup> data = commonUrlsGroupService.getAll();
            return ResponseEntity.ok(new ResultSuccess<>(data));
        }catch (Exception ex) {
            log.error("getMailbox exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
