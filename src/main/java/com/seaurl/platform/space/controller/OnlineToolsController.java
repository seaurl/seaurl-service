package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.request.OnlineToolsRequest;
import com.seaurl.platform.space.request.UserSoftwareRequest;
import com.seaurl.platform.space.request.UserToolsRequest;
import com.seaurl.platform.space.service.OnlineToolsService;
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
public class OnlineToolsController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    UsersFacade usersFacade;

    @Autowired
    OnlineToolsService onlineToolsService;


    /**
     * 添加自定义工具
     * @param onlineToolsRequest
     * @return
     */
    @PostMapping("/crud/onlineTools/add")
    public ResponseEntity<?> addUserTools(@RequestBody OnlineToolsRequest onlineToolsRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            String name = onlineToolsRequest.getName();
            String character = onlineToolsRequest.getCharacter();
            String groupId = onlineToolsRequest.getGroupId();
            String url = onlineToolsRequest.getUrl();

            if (!StringUtils.hasLength(name) || !StringUtils.hasLength(character) || !StringUtils.hasLength(groupId) || !StringUtils.hasLength(url)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Date current = new Date();

            OnlineTools onlineTools = new OnlineTools();
            onlineTools.setUid(loginUser.getUid());

            onlineTools.setCharacter(character);
            onlineTools.setName(name);
            onlineTools.setUrl(url);
            onlineTools.setType("user");
            onlineTools.setGroupId(groupId);
            onlineTools.setCreatedDt(current);
            onlineTools.setUpdatedDt(current);

            boolean result = onlineToolsService.add(onlineTools);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("getMailbox exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除用户自定义工具
     * @param id
     * @return
     */
    @DeleteMapping("/crud/onlineTools/delete/{id}")
    public ResponseEntity<?> deleteUserEvent(@PathVariable String id) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(id)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Boolean result = onlineToolsService.remove(loginUser.getUid(), id);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("deleteUserEvent exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }

    }

//    @GetMapping("/info/{userName}/userTools")
//    public Mono<ResponseEntity<?>> getUserTools(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                List<OnlineTools> onlineToolsList = onlineToolsService.queryById(user.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(onlineToolsList));
//            });
//        }catch (Exception ex) {
//            log.error("getUserTools exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }

    /**
     *
     * @return
     */
    @GetMapping("/crud/onlineToolsGroup")
    public ResponseEntity<?> getSoftwareGroup() {
        try {
            List<OnlineToolsGroup> onlineToolsGroups = onlineToolsService.queryAll();
            return ResponseEntity.ok(new ResultSuccess<>(onlineToolsGroups));
        }catch (Exception ex) {
            log.error("getSoftwareGroup exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 在线工具
     * @param userToolsRequest
     * @return
     */
    @PostMapping("/crud/onlineTools/queryBy")
    public ResponseEntity<?> getSoftwareGroup(@RequestBody UserToolsRequest userToolsRequest){
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();

            String groupId = userToolsRequest.getGroupId();
            String param = userToolsRequest.getParam();
            String type = userToolsRequest.getType();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(type) || !StringUtils.hasLength(groupId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            if (type.equals("system")){
                List<OnlineTools> onlineToolsList = onlineToolsService.queryByParam(groupId, param);
                return ResponseEntity.ok(new ResultSuccess<>(onlineToolsList));
            }
            if(type.equals("user")){
                List<OnlineTools> onlineToolsList = onlineToolsService.queryByParam(loginUser.getUid(), groupId, param);
                return ResponseEntity.ok(new ResultSuccess<>(onlineToolsList));
            }
            return ResponseEntity.ok(new ResultSuccess<>(ResultStatus.DATA_EMPTY));
        }catch (Exception ex) {
            log.error("getSoftwareGroup exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
