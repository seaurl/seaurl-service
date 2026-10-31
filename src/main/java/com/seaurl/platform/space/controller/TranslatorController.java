package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.request.TranslatorRequest;
import com.seaurl.platform.space.service.TranslatorService;

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
@RequestMapping("/space")

public class TranslatorController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    TranslatorService translatorService;

    /**
     * 开始翻译
     * @param translatorRequest
     * @return
     */
    @PostMapping("/crud/translator/exec")
    public ResponseEntity<?> translator(@RequestBody TranslatorRequest translatorRequest){
        String sourceTxt = translatorRequest.getSourceTxt();
        String sourceLng = translatorRequest.getSourceLng();
        String targetLng = translatorRequest.getTargetLng();


        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(sourceTxt) || !StringUtils.hasLength(sourceLng) || !StringUtils.hasLength(targetLng)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try{
            String result = translatorService.startTranslation(sourceLng, targetLng, sourceTxt);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex){
            log.error("getGithubData error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 检测语言
     * @param translatorRequest
     * @return
     */
    @PostMapping("/crud/translator/detect")
    public ResponseEntity<?> detect(@RequestBody TranslatorRequest translatorRequest){
        String sourceTxt = translatorRequest.getSourceTxt();


        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(sourceTxt)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try{
            String result = translatorService.detectLanguage(sourceTxt);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex){
            log.error("getGithubData error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
