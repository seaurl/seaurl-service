//package com.seaurl.platform.space.controller;
//
//import com.seaurl.common.result.ResultStatus;
//import com.seaurl.common.result.ResultInfo;
//import com.seaurl.common.result.ResultSuccess;
//import com.seaurl.platform.space.model.OnlineTools;
//import com.seaurl.platform.common.model.User;
//import com.seaurl.platform.space.model.UserTools;
//import com.seaurl.platform.space.request.UserToolsRequest;
//import com.seaurl.platform.space.service.ToolsService;
//import com.seaurl.platform.space.service.UserService;
//import com.seaurl.platform.space.service.UserToolsService;
//import com.seaurl.platform.space.service.client.AccountFeignClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.*;
//
//@RestController
//public class UserToolsController {
//
//    @Autowired
//    UserToolsService userToolsService;
//
//
//    @Autowired
//    UserService userService;
//
//    @Autowired
//    ToolsService toolsService;
//
//    @Autowired
//    AccountFeignClient accountFeignClient;
//
//    /**
//     * 添加usertools
//     *
//     * @param userToolsRequest
//     * @return
//     */
//    @PostMapping("/crud/userTools/batch/add")
//    public ResponseEntity<?> addBatchUserTools(@RequestBody UserToolsRequest userToolsRequest) {
//        String userName = userToolsRequest.getUserName();
//        String toolIds = userToolsRequest.getToolIds();
//        if (!StringUtils.hasLength(toolIds) || !StringUtils.hasLength(userName)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//        String[] toolIdStr = toolIds.split(",");
//        ArrayList<String> toolIdList = new ArrayList<>();
//        Collections.addAll(toolIdList, toolIdStr);
//        User user = accountFeignClient.getAccountInfoByUserName(userName);
//
//        Date current = new Date();
//
//        ArrayList<UserTools> userToolsList = new ArrayList<>();
//        toolIdList.forEach(item -> {
//            UserTools userTools = new UserTools();
//            userTools.setUid(user.getUid());
//            userTools.setToolId(item);
//            userTools.setCreatedDt(current);
//            userTools.setUpdatedDt(current);
//            userToolsList.add(userTools);
//        });
//
//        boolean result = userToolsService.addAll(userToolsList);
//        return ResponseEntity.ok(new ResultSuccess<>(result));
//    }
//
//    @PostMapping("/crud/userTools/addOrRemove")
//    public ResponseEntity<?> addUserTools(@RequestBody UserToolsRequest userToolsRequest) {
//        String userName = userToolsRequest.getUserName();
//        String toolId = userToolsRequest.getToolId();
//        if (!StringUtils.hasLength(toolId) || !StringUtils.hasLength(userName)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//        User user = accountFeignClient.getAccountInfoByUserName(userName);
//        UserTools queryUserTools = userToolsService.queryBy(user.getUid(), toolId);
//        // 判断是否已存在，不存在就添加 ，否则 就删除
//        if (queryUserTools == null) {
//            Date current = new Date();
//            UserTools userTools = new UserTools();
//            userTools.setUid(user.getUid());
//            userTools.setToolId(toolId);
//            userTools.setCreatedDt(current);
//            userTools.setUpdatedDt(current);
//
//            boolean result = userToolsService.add(userTools);
//            return ResponseEntity.ok(new ResultSuccess<>(result));
//        } else {
//            boolean result = userToolsService.remove(user.getUid(), toolId);
//            return ResponseEntity.ok(new ResultSuccess<>(result));
//        }
//    }
//
//    /**
//     * 删除userTools
//     * @param userToolsRequest
//     * @return
//     */
//    @PostMapping("/crud/userTools/remove")
//    public ResponseEntity<?> removeUserTools(@RequestBody UserToolsRequest userToolsRequest) {
//        String userName = userToolsRequest.getUserName();
//        String toolId = userToolsRequest.getToolId();
//        if (!StringUtils.hasLength(toolId) || !StringUtils.hasLength(userName)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//
//        User user = accountFeignClient.getAccountInfoByUserName(userName);
//        boolean result = userToolsService.remove(user.getUid(), toolId);
//        return ResponseEntity.ok(new ResultSuccess<>(result));
//    }
//
//    /**
//     * @return
//     */
//    @GetMapping("/info/tools")
//    public ResponseEntity<?> getTools() {
//        List<OnlineTools> toolsList = toolsService.getAll();
//        return ResponseEntity.ok(new ResultSuccess<>(toolsList));
//    }
//
//    /**
//     * 获取用户小工具(单表查询)
//     * @param userName
//     * @return
//     */
//    @GetMapping("/info/{userName}/userTools")
//    public ResponseEntity<?> getUserTools(@PathVariable String userName) {
//        if (!StringUtils.hasLength(userName)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//
//        User user = accountFeignClient.getAccountInfoByUserName(userName);
//
//        List<UserTools> userTools = userToolsService.queryByUid(user.getUid());
//        return ResponseEntity.ok(new ResultSuccess<>(userTools));
//    }
//
//    /**
//     *获取用户小工具(多表查询)
//     * @param userName
//     * @return
//     */
//    @GetMapping("/info/{userName}/fullUserTools")
//    public ResponseEntity<?> getFullUserTools(@PathVariable String userName) {
//        if (!StringUtils.hasLength(userName)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//
//        User user = accountFeignClient.getAccountInfoByUserName(userName);
//
//        List<HashMap> userTools = userToolsService.queryUserTools(user.getUid());
//        return ResponseEntity.ok(new ResultSuccess<>(userTools));
//    }
//}
