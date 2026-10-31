package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.AiTools;
import com.seaurl.platform.space.model.ChatAiCategory;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.request.AiToolsRoleRequest;
import com.seaurl.platform.space.request.ChatAiCategoryRequest;
import com.seaurl.platform.space.service.AiToolsService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/space")

public class AiToolsController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    AiToolsService aiToolsService;


    /**
     * @param aiToolsRoleRequest
     * @return
     */
    @PostMapping("/crud/aiTools/role/add")
    public ResponseEntity<?> addCategory(@RequestBody AiToolsRoleRequest aiToolsRoleRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String groupId = aiToolsRoleRequest.getGroupId();
        String name = aiToolsRoleRequest.getName();
        String command = aiToolsRoleRequest.getCommand();
        String background = aiToolsRoleRequest.getBackground();

        String desc = aiToolsRoleRequest.getDesc();
        String avatar = aiToolsRoleRequest.getAvatar();

        if (!StringUtils.hasLength(groupId) || !StringUtils.hasLength(name) || !StringUtils.hasLength(command) || !StringUtils.hasLength(background)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }


        try {
            AiTools aiTools = new AiTools();
            aiTools.setUid(loginUser.getUid());
            aiTools.setGroupId(groupId);
            aiTools.setAvatar(avatar);
            aiTools.setName(name);
            aiTools.setDesc(desc);
            aiTools.setCommand(command);
            aiTools.setBackground(background);

            // 添加的角色默认就用ds-v3
            aiTools.setType("ai-deepseek-v3-241226");
            aiTools.setModel("deepseek-v3-241226");
            aiTools.setEndpoint("ep-20250228194503-smxcr");
            aiTools.setInvoke("https://ark.cn-beijing.volces.com/api/v3");
            aiTools.setOrigin("volcengine");

            AiTools result = aiToolsService.add(aiTools);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.error("addCategory error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    @PostMapping("/crud/aiTools/role/edit")
    public ResponseEntity<?> editCategory(@RequestBody AiToolsRoleRequest aiToolsRoleRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String id = aiToolsRoleRequest.getId();
        String name = aiToolsRoleRequest.getName();
        String command = aiToolsRoleRequest.getCommand();
        String background = aiToolsRoleRequest.getBackground();

        String desc = aiToolsRoleRequest.getDesc();
        String avatar = aiToolsRoleRequest.getAvatar();

        if (!StringUtils.hasLength(id) || !StringUtils.hasLength(name) || !StringUtils.hasLength(command) || !StringUtils.hasLength(background)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }


        try {
            AiTools aiTools = new AiTools();
            aiTools.setUid(loginUser.getUid());
            aiTools.setId(id);
            aiTools.setAvatar(avatar);
            aiTools.setName(name);
            aiTools.setDesc(desc);
            aiTools.setCommand(command);
            aiTools.setBackground(background);

            // 添加的角色默认就用ds-v3
//            aiTools.setType("ai-deepseek-v3-241226");
//            aiTools.setModel("deepseek-v3-241226");
//            aiTools.setEndpoint("ep-20250228194503-smxcr");
//            aiTools.setInvoke("https://ark.cn-beijing.volces.com/api/v3");
//            aiTools.setOrigin("volcengine");

            Boolean result = aiToolsService.update(aiTools);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.error("addCategory error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    @DeleteMapping("/crud/aiTools/role/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable String id) {

        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(id)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            Boolean result = aiToolsService.delete(loginUser.getUid(), id);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("aiTools query error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     * 查询所有的aitools
     *
     * @return
     */
    @GetMapping("/crud/aiTools/queryAll")
    public ResponseEntity<?> queryAll() {

        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        try {
            HashMap result = aiToolsService.queryByUid(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("aiTools query error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }
}
