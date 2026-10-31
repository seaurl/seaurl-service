package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.request.UserCommonUrlsRequest;
import com.seaurl.platform.space.request.UserSoftwareRequest;
import com.seaurl.platform.space.service.SoftwareService;
import com.seaurl.platform.space.service.facade.UsersFacade;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/space")

@Observed
public class SoftwareController {
    private static Logger log = LoggerFactory.getLogger(SoftwareController.class);

    @Autowired
    SoftwareService softwareService;

    @Autowired
    UsersFacade usersFacade;

    /**
     * 根据用户名添加和删除software
     * @param userSoftwareRequest
     * @return
     */
    @PostMapping("/crud/userSoftware/addOrRemove")
    public ResponseEntity<?> addUserSoftware(@RequestBody UserSoftwareRequest userSoftwareRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String softwareId = userSoftwareRequest.getSoftwareId();
            if (!StringUtils.hasLength(softwareId) ) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            UserSoftware userSoftware = softwareService.queryBy(loginUser.getUid(), softwareId);
            // 判断是否已存在，不存在就添加 ，否则 就删除
            if (userSoftware == null) {
                Date current = new Date();
                UserSoftware newUserSoftware = new UserSoftware();
                newUserSoftware.setUid(loginUser.getUid());
                newUserSoftware.setSoftwareId(softwareId);
                newUserSoftware.setCreatedDt(current);
                newUserSoftware.setUpdatedDt(current);

                boolean result = softwareService.add(newUserSoftware);
                return ResponseEntity.ok(new ResultSuccess<>(result));
            } else {
                boolean result = softwareService.remove(loginUser.getUid(), softwareId);
                return ResponseEntity.ok(new ResultSuccess<>(result));
            }
        }catch (Exception ex) {
            log.error("addUserSoftware exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 根据用户名查询用户的software
     * @param userName
     * @return
     */
//    @GetMapping("/info/{userName}/userSoftware")
//    public Mono<ResponseEntity<?>> getUserSoftware(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                List<Software> softwares = softwareService.queryById(user.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(softwares));
//            });
//        }catch (Exception ex) {
//            log.error("getUserSoftware exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//
//    }

    /**
     * 获取所有software group组
     * @return
     */
    @GetMapping("/info/softwareGroup")
    public ResponseEntity<?> getSoftwareGroup() {
        try {
            List<SoftwareGroup> softwareGroupList = softwareService.queryAll();
            return ResponseEntity.ok(new ResultSuccess<>(softwareGroupList));
        }catch (Exception ex) {
            log.error("getSoftwareGroup exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据groupId和param获取所有software
     * @param userSoftwareRequest
     * @return
     */
    @PostMapping("/info/software/queryBy")
    public ResponseEntity<?> getSoftwareGroup(@RequestBody UserSoftwareRequest userSoftwareRequest) {
        try {
            String groupId = userSoftwareRequest.getGroupId();
            String param = userSoftwareRequest.getParam();
            List<Software> softwares = softwareService.queryByParam(groupId, param);
            log.info("softwares list sie={}", softwares.size());
            return ResponseEntity.ok(new ResultSuccess<>(softwares));
        }catch (Exception ex) {
            log.error("getSoftwareGroup exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
