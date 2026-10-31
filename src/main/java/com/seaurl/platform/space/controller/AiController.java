package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContextHolder;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.model.AiConfig;
import com.seaurl.platform.space.model.AiMessage;
import com.seaurl.platform.space.request.AiRequest;
import com.seaurl.platform.space.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/space")
public class AiController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    AiService aiService;

    @GetMapping("/crud/ai/config")
    public ResponseEntity<?> getConfig() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            AiConfig config = aiService.getConfig(loginUser.getUid());
            Map<String, Object> data = new HashMap<>();
            if (config != null) {
                data.put("model", config.getModel());
                data.put("baseUrl", config.getBaseUrl());
                data.put("hasKey", StringUtils.hasLength(config.getApiKey()));
            } else {
                data.put("model", "");
                data.put("baseUrl", "");
                data.put("hasKey", false);
            }
            return ResponseEntity.ok(new ResultSuccess<>(data));
        } catch (Exception ex) {
            log.error("ai getConfig exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/ai/config/save")
    public ResponseEntity<?> saveConfig(@RequestBody AiRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getModel()) || !StringUtils.hasLength(request.getApiKey())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            AiConfig result = aiService.saveConfig(
                    loginUser.getUid(),
                    request.getModel(),
                    request.getApiKey(),
                    request.getBaseUrl());
            Map<String, Object> data = new HashMap<>();
            data.put("model", result.getModel());
            data.put("baseUrl", result.getBaseUrl());
            data.put("hasKey", true);
            return ResponseEntity.ok(new ResultSuccess<>(data));
        } catch (Exception ex) {
            log.error("ai saveConfig exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/ai/key")
    public ResponseEntity<?> getKey() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            AiConfig config = aiService.getConfig(loginUser.getUid());
            Map<String, Object> data = new HashMap<>();
            data.put("apiKey", config != null ? config.getApiKey() : "");
            data.put("model", config != null ? config.getModel() : "");
            data.put("baseUrl", config != null ? config.getBaseUrl() : "");
            return ResponseEntity.ok(new ResultSuccess<>(data));
        } catch (Exception ex) {
            log.error("ai getKey exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/ai/message/save")
    public ResponseEntity<?> saveMessage(@RequestBody AiRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getConversationId()) || !StringUtils.hasLength(request.getRole())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            AiMessage result = aiService.saveMessage(
                    loginUser.getUid(),
                    request.getConversationId(),
                    request.getRole(),
                    request.getContent(),
                    request.getTitle());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("ai saveMessage exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/ai/messages")
    public ResponseEntity<?> getMessages(@RequestParam("conversationId") String conversationId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<AiMessage> messages = aiService.getMessages(loginUser.getUid(), conversationId);
            return ResponseEntity.ok(new ResultSuccess<>(messages));
        } catch (Exception ex) {
            log.error("ai getMessages exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/ai/conversations")
    public ResponseEntity<?> getConversations() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<String> conversationIds = aiService.getConversations(loginUser.getUid());
            List<Map<String, Object>> result = new ArrayList<>();
            for (String cid : conversationIds) {
                List<AiMessage> msgs = aiService.getMessages(loginUser.getUid(), cid);
                if (!msgs.isEmpty()) {
                    AiMessage first = msgs.get(0);
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("id", cid);
                    conv.put("title", first.getTitle() != null ? first.getTitle() : first.getContent());
                    conv.put("messageCount", msgs.size());
                    conv.put("time", first.getCreatedDt());
                    result.add(conv);
                }
            }
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("ai getConversations exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/ai/conversation/delete")
    public ResponseEntity<?> deleteConversation(@RequestParam("conversationId") String conversationId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            aiService.deleteConversation(loginUser.getUid(), conversationId);
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.error("ai deleteConversation exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/ai/conversation/all/delete")
    public ResponseEntity<?> deleteAllConversations() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            aiService.deleteAllConversations(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.error("ai deleteAllConversations exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
