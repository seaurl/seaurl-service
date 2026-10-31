package com.seaurl.platform.users.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.users.request.VerifyRequest;
import com.seaurl.platform.users.utils.AliyunVerify;
import com.seaurl.platform.users.utils.InputHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/users")

public class VerifyController {
    private Logger log = LoggerFactory.getLogger(getClass());


    @Autowired
    private AliyunConfig aliyunConfig;

    /**
     * @param verifyRequest
     * @return
     */
    @PostMapping("/info/verify/code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyRequest verifyRequest) {
        String captchaVerifyParam = verifyRequest.getCaptchaVerifyParam();
        String sceneId = verifyRequest.getSceneId();
        if (!StringUtils.hasLength(captchaVerifyParam) || !StringUtils.hasLength(sceneId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            //校验
            AliyunVerify aliyunVerify = new AliyunVerify();
            boolean verifyResult = aliyunVerify.verify(aliyunConfig.getAccessKeyId(), aliyunConfig.getAccessKeySecret(), sceneId, captchaVerifyParam);
            log.info("verifyResult ={}", verifyResult);
            return ResponseEntity.ok(new ResultSuccess<>(verifyResult));
        } catch (Exception ex) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.VERIFIED_FAILURE));
        }
    }
}
