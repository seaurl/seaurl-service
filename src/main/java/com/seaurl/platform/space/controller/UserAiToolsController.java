package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.model.UserAiTools;
import com.seaurl.platform.space.request.ReportRequest;
import com.seaurl.platform.space.request.UserAiToolsRequest;
import com.seaurl.platform.space.service.UserAiToolsService;
import org.bson.types.ObjectId;
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

public class UserAiToolsController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    UserAiToolsService userAiToolsService;

    /**
     * 查询usedId 查询不到就新增，
     *
     * @param aiToolId
     * @return 最后返回usedId
     */
    @GetMapping("/crud/userAiTools/queryUsedId")
    public ResponseEntity<?> queryUsedId(@RequestParam("aiToolId") String aiToolId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(aiToolId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        log.info("fogo is here ={}", aiToolId);
        try {
            List<UserAiTools> aitoolsList = userAiToolsService.queryBy(loginUser.getUid(), aiToolId);

            if (!aitoolsList.isEmpty()) {
                UserAiTools firstElement = aitoolsList.get(0); // 获取第一个元素
                String usedId = firstElement.getUsedId();// 获取名为"name"的字段值
                return ResponseEntity.ok(new ResultSuccess<>(usedId));
            } else {
                Date date = new Date();
                ObjectId newId = new ObjectId();

                UserAiTools userAiTools = new UserAiTools();

                userAiTools.setUsedId(newId.toString());
                userAiTools.setAiToolId(aiToolId);
                userAiTools.setUid(loginUser.getUid());
                userAiTools.setCreatedDt(date);
                userAiTools.setUpdatedDt(date);

                userAiToolsService.add(userAiTools);

                return ResponseEntity.ok(new ResultSuccess<>(newId.toString()));
            }
        } catch (Exception ex) {
            log.error("queryUsedId queryAll exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 查询我最近使用的ai与工具
     *
     * @return
     */
    @GetMapping("/crud/userAiTools/query")
    public ResponseEntity<?> queryAll() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<HashMap> result = userAiToolsService.queryBy(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("userAiTools queryAll exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param userAiToolsRequest
     * @return
     */
    @PostMapping("/crud/userAiTools/add")
    public ResponseEntity<?> add(@RequestBody UserAiToolsRequest userAiToolsRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        String aiToolId = userAiToolsRequest.getAiToolId();

        if (!StringUtils.hasLength(aiToolId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            Date date = new Date();
            ObjectId newId = new ObjectId();

            UserAiTools userAiTools = new UserAiTools();

            userAiTools.setUsedId(newId.toString());
            userAiTools.setAiToolId(aiToolId);
            userAiTools.setUid(loginUser.getUid());
            userAiTools.setCreatedDt(date);
            userAiTools.setUpdatedDt(date);

            UserAiTools result = userAiToolsService.add(userAiTools);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("userAiTools add exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除
     *
     * @param userAiToolsRequest
     * @return
     */
    @GetMapping("/crud/userAiTools/delete")
    public ResponseEntity<?> delete(@RequestParam String usedId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(usedId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = userAiToolsService.delete(usedId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("userAiTools delete exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 更新时间
     * @param usedId
     * @return
     */
    @GetMapping("/crud/userAiTools/updateDt")
    public ResponseEntity<?> updateDt(@RequestParam String usedId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(usedId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = userAiToolsService.updateDt(usedId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("userAiTools delete exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 设置是否置顶
     *
     * @param userAiToolsRequest
     * @return
     */
    @PostMapping("/crud/userAiTools/setTop")
    public ResponseEntity<?> setTop(@RequestBody UserAiToolsRequest userAiToolsRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String userAiToolId = userAiToolsRequest.getUserAiToolId();
        Boolean isTop = userAiToolsRequest.getIsTop();

        if (isTop == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        if (!StringUtils.hasLength(userAiToolId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            UserAiTools userAiTools = new UserAiTools();
            userAiTools.setId(userAiToolId);
            userAiTools.setIsTop(isTop);
            boolean result = userAiToolsService.setTop(userAiTools);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("userAiTools delete exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
