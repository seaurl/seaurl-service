package com.seaurl.platform.space.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.request.FileRequest;
import com.seaurl.platform.space.service.FileService;
import com.seaurl.platform.space.service.FileShareService;
import com.seaurl.platform.space.service.facade.UsersFacade;
import com.seaurl.platform.common.notification.NotificationDowngradeService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")

public class FileShareController {

    private Logger log = LoggerFactory.getLogger(getClass());


    @Autowired
    FileShareService fileShareService;

    @Autowired
    FileService fileService;

    @Autowired
    NotificationDowngradeService notificationDowngradeService;

    @Autowired
    UsersFacade usersFacade;

//    /**
//     * @param fileId
//     * @return
//     */
//    @GetMapping("/crud/file/getSharedFriends")
//    public ResponseEntity<?> getSharedFriends(@RequestParam String fileId) {
//        LoginUser loginUser = UserContextHolder.getContext().getUser();
//        if (loginUser == null) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
//        }
//
//        if (!StringUtils.hasLength(fileId)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//        try {
//            FileShare result = fileShareService.queryBy(loginUser.getUid(), fileId);
//            return ResponseEntity.ok(new ResultSuccess<>(result));
//        } catch (Exception ex) {
//            log.info("getShareFileFriends ex={}", ex.getMessage());
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
//        }
//    }

//    /**
//     * @param fileRequest
//     * @return
//     */
//    @PostMapping("/crud/file/sendNotifyToFriends")
//    public ResponseEntity<?> sendNotifyToFriends(@RequestBody FileRequest fileRequest) {
//        LoginUser loginUser = UserContextHolder.getContext().getUser();
//        if (loginUser == null) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
//        }
//        String fileId = fileRequest.getFileId();
//        List<String> fids = fileRequest.getFids();
//
//        if (!StringUtils.hasLength(fileId) || fids.isEmpty()) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//
//        try {
//            File file = fileService.queryFileById(fileId);
//            if (file != null) {
//                Mono<List<NotifyRemind>> listMono = notificationDowngradeService.queryNotifyBy(loginUser, loginUser.getUid(), fileId, fids);
//
//                listMono.subscribe(res -> {
//                    log.info("result:{}", res);
////                    返回为空则全部发送
//                    if (res.isEmpty()) {
//                        log.info("--- 全部发送 ---");
//                        for (String fid : fids) {
//                            sendNotify(loginUser, file, fid);
//                        }
//                    } else {
//                        log.info("--- 部分发送或全部不发送 ---");
//                        // 找出 fids 中不存在于 notifyRemind.id 的值
//                        List<String> missingIds = fids.stream()
//                                .filter(fid -> res.stream().noneMatch(recipient -> recipient.getRecipientId().equals(fid)))
//                                .collect(Collectors.toList());
//                        log.info("--- missingIds --- {}", JSONArray.toJSONString(missingIds));
//                        for (String fid : missingIds) {
//                            sendNotify(loginUser, file, fid);
//                        }
//                    }
//                });
//                return ResponseEntity.ok(new ResultSuccess<>(true));
//            } else {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UN_QUERY_DATA));
//            }
//        } catch (Exception ex) {
//            log.info("sendNotifyToFriends ex ={}", ex.getMessage());
//            return ResponseEntity.ok(new ResultInfo<>(ex.getMessage()));
//        }
//    }
//
//    /**
//     * @param loginUser
//     * @param file
//     * @param friendId
//     */
//    private void sendNotify(LoginUser loginUser, File file, String friendId) {
//
//        // 查询用户
//        Mono<User> userMono = usersFacade.getAccountInfoByUseId(friendId);
//
//        userMono.subscribe(user -> {
//            // 如果用户有打开shareFileToMe则发送消息
//            // 如果用户设置了不接收分享文件通知就不能发送
//            if (user != null && user.getNotices().contains("shareFileToMe")) {
//                log.info("--- 用户打开了消息通知，开始发送消息给用户 ---");
//                NotifyRemind newNr = new NotifyRemind();
//                ObjectId remindId = new ObjectId();
//                Date date = new Date();
//
//                String action = "分享了文件给你";
//
//                newNr.setRemindId(remindId.toString());
//                newNr.setSenderId(loginUser.getUid());
//                newNr.setSenderName(loginUser.getUserName());
//                newNr.setSenderAvatar(loginUser.getAvatar());
//                newNr.setSenderAction(action);
//                newNr.setObjectId(file.getId());
//                newNr.setObject(file.getName());
//                newNr.setObjectType("file");
//                newNr.setRecipientId(friendId);
//
//                // 模板语法，如{张三}复制了你的网址 <a>淘宝</a>
//                newNr.setMessage("{}" + action);
//                newNr.setIsRead(false);
//                newNr.setCreatedDt(date);
//                newNr.setUpdatedDt(date);
//
//                String sendInfo = JSON.toJSONString(newNr);
//                Message<String> message = MessageBuilder.withPayload(sendInfo).build();
//                log.info("send shareFileRemind start={}", sendInfo);
//                streamBridge.send("shareFileRemind-out-0", message);
//            }else{
//                log.info("查询不到用户或者用户没有打开shareFileToMe通知");
//            }
//        });
//    }
}
