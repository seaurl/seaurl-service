package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.ChatAiCategory;
import com.seaurl.platform.space.model.ChatAiMessage;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.request.ChatAiCategoryRequest;
import com.seaurl.platform.space.service.ChatAiCategoryService;
import com.seaurl.platform.space.service.ChatAiMessageService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/space")

public class ChatAiCategoryController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ChatAiCategoryService chatAiCategoryService;

    @Autowired
    ChatAiMessageService chatAiMessageService;

    /**
     * 添加聊天分类
     * @param chatAiRequest
     * @return
     */
    @PostMapping("/crud/chatAi/category/add")
    public ResponseEntity<?> addCategory(@RequestBody ChatAiCategoryRequest chatAiCategoryRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String title = chatAiCategoryRequest.getTitle();

        try {
            if (!StringUtils.hasLength(title)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Date current = new Date();
            ObjectId newId = new ObjectId();
            ChatAiCategory chatAiCategory = new ChatAiCategory();
            chatAiCategory.setUid(loginUser.getUid());
            chatAiCategory.setChatAiId(newId.toString());
            chatAiCategory.setTitle(title);
            chatAiCategory.setIsTop(false);
            chatAiCategory.setCreatedDt(current);
            chatAiCategory.setUpdatedDt(current);

            ChatAiCategory result = chatAiCategoryService.add2(chatAiCategory);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("addCategory error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     *
     * @return
     */
    @GetMapping("/crud/chatAi/category/query")
    public ResponseEntity<?> queryCategory() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        try {
            List<ChatAiCategory> result = chatAiCategoryService.queryByUid(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("queryCategory error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     *
     * @param chatAiRequest
     * @return
     */
    @PostMapping("/crud/chatAi/category/update")
    public ResponseEntity<?> updateCategory(@RequestBody ChatAiCategoryRequest chatAiCategoryRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        String title = chatAiCategoryRequest.getTitle();
        String cid = chatAiCategoryRequest.getCid();
        try {
            if (!StringUtils.hasLength(title) || !StringUtils.hasLength(cid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            ChatAiCategory chatAiCategory = new ChatAiCategory();
            chatAiCategory.setUid(loginUser.getUid());
            chatAiCategory.setId(cid);
            chatAiCategory.setTitle(title);
            boolean result = chatAiCategoryService.edit(chatAiCategory);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.error("updateCategory error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     * 设置是否置顶
     * @param chatAiRequest
     * @return
     */
    @PostMapping("/crud/chatAi/category/setTop")
    public ResponseEntity<?> setTop(@RequestBody ChatAiCategoryRequest chatAiCategoryRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        Boolean isTop = chatAiCategoryRequest.getIsTop();
        String cid = chatAiCategoryRequest.getCid();
        try {
            if (!StringUtils.hasLength(cid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            ChatAiCategory chatAiCategory = new ChatAiCategory();
            chatAiCategory.setUid(loginUser.getUid());
            chatAiCategory.setId(cid);
            chatAiCategory.setIsTop(isTop);
            boolean result = chatAiCategoryService.setTop(chatAiCategory);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.error("updateCategory error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     * 删除分类，并且删除该分类下的所有聊天记录
     * @param id
     * @return
     */
    @DeleteMapping("/crud/chatAi/category/delete/{chatAiId}")
    public ResponseEntity<?> deleteCategory(@PathVariable String chatAiId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            if (!StringUtils.hasLength(chatAiId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = chatAiCategoryService.remove(loginUser.getUid(), chatAiId);
            log.info("删除分类：{}", result);
            if (result){
                // 消息可能为空，所以当没有消息时，删除返回的结果可能是false
                boolean result2 = chatAiMessageService.removeByChatAiId(loginUser.getUid(), chatAiId);
//                log.info("删除分类2：{}", result2);
                return ResponseEntity.ok(new ResultSuccess<>(true));
            }
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }catch (Exception ex) {
            log.error("deleteCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
