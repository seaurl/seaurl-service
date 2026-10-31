package com.seaurl.platform.space.controller;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.model.chat.ChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.space.config.BaiduYunConfig;
import com.seaurl.platform.space.config.DeepseekConfig;
import com.seaurl.platform.space.config.DoubaoConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.request.ChatAiRequest;
import com.seaurl.platform.space.response.aiSpace.AiMessageResponse;
import com.seaurl.platform.space.service.AiToolsService;
import com.seaurl.platform.space.service.ChatAiCategoryService;
import com.seaurl.platform.space.service.ChatAiMessageService;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/space")

public class ChatAiMessageController {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    ChatAiCategoryService chatAiCategoryService;

    @Autowired
    ChatAiMessageService chatAiMessageService;

    @Autowired
    AiToolsService aiToolsService;

    @Autowired
    AliyunConfig aliyunConfig;

    @Autowired
    BaiduYunConfig baiduYunConfig;

    @Autowired
    DeepseekConfig deepseekConfig;

    @Autowired
    DoubaoConfig doubaoConfig;

    private final Gson gson = new Gson();

    @GetMapping(value = "/info/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getChatResponse() {
        return Flux.create(emitter -> {
            Qianfan qianfan = new Qianfan(baiduYunConfig.getAccessKeyId(), baiduYunConfig.getAccessKeySecret());

            qianfan.chatCompletion()
                    .model("ERNIE-4.0-8K")
                    .addMessage("user", "你好")
                    // 启用流式返回
                    .executeStream()
                    .forEachRemaining(chunk -> emitter.next(gson.toJson(chunk)));
            emitter.complete();
        });
    }

    // 将文心一言、通义千问或者其它ai输出的结果转换成通用结果
    private Flux<AiMessageResponse> aiMessageResponseSwitch(String uid, AiTools aiTools) throws NoApiKeyException, InputRequiredException {

        // 根据chatAiId查询前10条数据
        List<ChatAiMessage> chatAiMessages = chatAiMessageService.queryMessage(uid, aiTools.getType(), aiTools.getId());
        //  恢复成正序
        Collections.reverse(chatAiMessages);
        // 判断返回的第1个元素type如果不是user，则删除
        // 注意：第1条消息必须是user或者system
        ChatAiMessage firstMessage = chatAiMessages.get(0);
        if (firstMessage.getRole().equals("bot")) {
            chatAiMessages.remove(0);
        }
        // 有通义千问、文心一言、还有AI角色，如ai-开头的
        if (aiTools.getOrigin().equals("aliyun")) {
            log.info("--- 正在使用通义千问聊天 ---");

            Generation gen = new Generation();
            List<Message> messages = new ArrayList<>();
            // 添加最近20条历史聊天记录
            if (!chatAiMessages.isEmpty()) {

                Message systemMsg = Message.builder().role(Role.SYSTEM.getValue()).content(aiTools.getCommand()).build();
                messages.add(systemMsg);

                chatAiMessages.forEach(message -> {
                    if (message.getRole().equals("user")) {
                        String _msg = message.getMessage();
                        if (message.getFile() != null && message.getFile().getSize() > 0 && StringUtils.hasLength(message.getFile().getContent())) {
                            _msg = "这是用户上传的文件内容：" + message.getFile().getContent() + "，这是用户的问题：" + _msg;
                        }
                        Message userMsg = Message
                                .builder()
                                .role(Role.USER.getValue())
                                .content(_msg)
                                .build();
                        messages.add(userMsg);
                    }
                    if (message.getRole().equals("bot")) {
                        Message assistantMsg = Message
                                .builder()
                                .role(Role.ASSISTANT.getValue())
                                .content(message.getMessage())
                                .build();
                        messages.add(assistantMsg);
                    }
                });
            }
            messages.forEach(msg -> {
                log.info("msg type ={}", msg.getRole());
            });


            GenerationParam param = GenerationParam.builder()
                    // 设置ai所属大模型
                    .model(aiTools.getModel())
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .enableSearch(true)
                    .incrementalOutput(true)
                    .build();

            Flowable<GenerationResult> generationResult = gen.streamCall(param);

            // 解析generationResult并转化成AiMessageResponse类型
            Flowable<AiMessageResponse> result = generationResult.map(genResult -> {
                AiMessageResponse response = new AiMessageResponse();

                GenerationOutput output = genResult.getOutput();
                List<GenerationOutput.Choice> choices = output.getChoices();

                // 遍历列表并访问每个 Choice 对象的属性
                for (GenerationOutput.Choice choice : choices) {
                    String finishReason = choice.getFinishReason();
                    Message message = choice.getMessage();

                    response.setIsEnd(finishReason.equals("stop") || finishReason.equals("length"));
                    response.setContent(message.getContent());
                }
                return response;
            });
            // 将Flowable转换成Flux类型，目的是为了统一
            return RxJava2Adapter.flowableToFlux(result);
        }
        // 文心一言
        if (aiTools.getOrigin().equals("baidu")) {
            log.info("--- 正在使用文心一言聊天 ---");

            return Flux.create(emitter -> {
                Qianfan qianfan = new Qianfan(baiduYunConfig.getAccessKeyId(), baiduYunConfig.getAccessKeySecret());

                List<com.baidubce.qianfan.model.chat.Message> messages = new ArrayList<>();
                // 添加最近20条历史聊天记录
                if (!chatAiMessages.isEmpty()) {
                    chatAiMessages.forEach(message -> {
                        if (message.getRole().equals("user")) {
                            String _msg = message.getMessage();
                            if (message.getFile() != null && message.getFile().getSize() > 0 && StringUtils.hasLength(message.getFile().getContent())) {
                                _msg = "这是用户上传的文件内容：" + message.getFile().getContent() + "，这是用户的问题：" + _msg;
                            }
                            com.baidubce.qianfan.model.chat.Message msg = new com.baidubce.qianfan.model.chat.Message();
                            msg.setContent(_msg);
                            msg.setRole("user");
                            messages.add(msg);
                        }
                        if (message.getRole().equals("bot")) {
                            com.baidubce.qianfan.model.chat.Message msg = new com.baidubce.qianfan.model.chat.Message();
                            msg.setContent(message.getMessage());
                            msg.setRole("assistant");
                            messages.add(msg);
                        }
                    });
                }
                messages.forEach(msg -> {
                    log.info("msg type ={}", msg.getRole());
                });
                qianfan.chatCompletion()
                        // 设置ai所属大模型
                        .model(aiTools.getModel())
                        .messages(messages)
                        // 启用流式返回
                        .executeStream()
                        .forEachRemaining(chunk -> {
                            // 将chunk转换成AiMessageResponse
                            AiMessageResponse response = new AiMessageResponse();
                            String result = chunk.getResult();
                            boolean isStop = chunk.getEnd();

                            response.setIsEnd(isStop);
                            response.setContent(result);

                            emitter.next(response);
                        });
                emitter.complete();
            });
        }
        // deepseek大模型
        if (aiTools.getOrigin().equals("deepseek")) {
            log.info("--- 正在使用deepseek聊天 ---");
            return callDeepSeek(chatAiMessages, aiTools);
        }
        // doubao豆包大模型
        if (aiTools.getOrigin().equals("volcengine")) {
            log.info("--- 正在使用豆包聊天 ---");

            return Flux.create(emitter -> {
                ArkService service;
                if (StringUtils.hasLength(aiTools.getInvoke())) {
                    service = ArkService.builder().baseUrl(aiTools.getInvoke()).apiKey(doubaoConfig.getAccessKeySecret()).build();
                } else {
                    service = ArkService.builder().apiKey(doubaoConfig.getAccessKeySecret()).build();
                }


                // 构建消息列表
                List<ChatMessage> streamMessages = new ArrayList<>();

                if (!chatAiMessages.isEmpty()) {
                    // 添加系统指令
                    streamMessages.add(ChatMessage.builder()
                            .role(ChatMessageRole.SYSTEM)
                            .content(aiTools.getCommand())
                            .build());

                    // 添加历史消息
                    chatAiMessages.forEach(message -> {
                        if (message.getRole().equals("user")) {
                            String _msg = message.getMessage();
                            if (message.getFile() != null && message.getFile().getSize() > 0 && StringUtils.hasLength(message.getFile().getContent())) {
                                _msg = "这是用户上传的文件内容：" + message.getFile().getContent() + "，这是用户的问题：" + _msg;
                            }
                            streamMessages.add(ChatMessage.builder()
                                    .role(ChatMessageRole.USER)
                                    .content(_msg)
                                    .build());
                        } else if (message.getRole().equals("bot")) {
                            streamMessages.add(ChatMessage.builder()
                                    .role(ChatMessageRole.ASSISTANT)
                                    .content(message.getMessage())
                                    .build());
                        }
                    });
                }


                // 构建请求
                ChatCompletionRequest streamChatCompletionRequest = ChatCompletionRequest.builder()
                        .model(aiTools.getEndpoint())
                        .messages(streamMessages)
                        .build();

                // 调用豆包大模型的流式 API
                service.streamChatCompletion(streamChatCompletionRequest)
                        .doOnError(Throwable::printStackTrace)
                        .blockingForEach(choice -> {
                            if (!choice.getChoices().isEmpty()) {
//                                log.info("--- choice:{}", choice);

                                // 将豆包的响应转换为 AiMessageResponse
                                AiMessageResponse response = new AiMessageResponse();
                                String content = choice.getChoices().get(0).getMessage().getContent().toString();
                                String reasonContent = ""; // deprecated: getReasoningContent not available in this SDK version
                                boolean isEnd = choice.getChoices().get(0).getFinishReason() != null
                                        && choice.getChoices().get(0).getFinishReason().equals("stop");

                                response.setContent(content);
                                response.setReasonContent(reasonContent);
                                response.setIsEnd(isEnd);

                                // 发送响应
                                emitter.next(response);

                                // 如果是结束标记，则完成流
                                if (isEnd) {
                                    emitter.complete();
                                }
                            }
                        });

                // 关闭服务
                service.shutdownExecutor();
            });
        }
        return null;
    }

    /**
     * 使用webclient调用deepseek api
     *
     * @param chatAiMessages
     * @param aiTools
     * @return
     */
    public Flux<AiMessageResponse> callDeepSeek(List<ChatAiMessage> chatAiMessages, AiTools aiTools) {
        // 初始化 WebClient
        WebClient webClient = WebClient.builder()
                .baseUrl(deepseekConfig.getAccessUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + deepseekConfig.getAccessKeySecret())
                .build();

        // 构建消息列表
        List<DeepSeekMessage> messages = new ArrayList<>();
        if (!chatAiMessages.isEmpty()) {
            messages.add(new DeepSeekMessage("system", aiTools.getCommand())); // 添加系统指令

            chatAiMessages.forEach(message -> {
                if (message.getRole().equals("user")) {
                    messages.add(new DeepSeekMessage("user", message.getMessage()));
                } else if (message.getRole().equals("bot")) {
                    messages.add(new DeepSeekMessage("assistant", message.getMessage()));
                }
            });
        }

        // 构建请求体
        DeepSeekRequest request = new DeepSeekRequest();
        request.setModel(aiTools.getModel());
        request.setMessages(messages);
        request.setStream(true); // 启用流式响应
        ObjectMapper objectMapper = new ObjectMapper();
        // 调用 DeepSeek API
        return webClient.post()
                .uri("/chat/completions")
                .body(Mono.just(request), DeepSeekRequest.class)
                .retrieve()
                .bodyToFlux(String.class) // 以字符串形式接收流式响应
                .<AiMessageResponse>handle((chunk, sink) -> {
//                    log.info("chunk:{}", chunk);
                    // 检查是否是结束标记 [DONE]
                    if ("[DONE]".equals(chunk.trim())) {
                        log.info("Received [DONE] signal, ending stream.");
                        sink.complete(); // 结束流
                        return;
                    }
                    // 解析每个 chunk 并转换为 AiMessageResponse
                    AiMessageResponse response = new AiMessageResponse();
                    // 假设 chunk 是一个 JSON 字符串，包含 "content" 和 "isEnd" 字段
                    // 这里需要根据 DeepSeek 的实际响应格式进行解析
                    JsonNode jsonNode;
                    try {
                        jsonNode = objectMapper.readTree(chunk);
                        String content = jsonNode.path("choices").path(0).path("delta").path("content").asText();
                        boolean isEnd = jsonNode.path("choices").path(0).path("finish_reason").asText().equals("stop");

                        response.setContent(content);
                        response.setIsEnd(isEnd);
                        sink.next(response);
                    } catch (JsonProcessingException e) {
                        log.info("转换报错：{}", e.getMessage());
                        sink.error(new RuntimeException(e));
                    }
                })
                .onErrorResume(e -> {
                    // 错误处理
                    log.error("DeepSeek API call failed: ", e);
                    return Flux.empty();
                });
    }


    /**
     * 拉取ai返回的消息
     *
     * @param chatAiRequest
     * @return
     */
    @PostMapping(value = "/crud/chatAi/pull", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> pull(@RequestBody ChatAiRequest chatAiRequest) {
        Constants.apiKey = aliyunConfig.getDashscopeApiKey();

        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        log.info("aiToolId ={}", chatAiRequest.getAiToolId());
        String aiToolId = chatAiRequest.getAiToolId();
        if (!StringUtils.hasLength(aiToolId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        // 查询aitools
        AiTools aiTools = aiToolsService.query(aiToolId);
        if (aiTools == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            Flux<AiMessageResponse> result = aiMessageResponseSwitch(loginUser.getUid(), aiTools);
            HttpHeaders headers = new HttpHeaders();
            // 设置成text/event-stream形式
            headers.setContentType(MediaType.TEXT_EVENT_STREAM);
            return new ResponseEntity<>(result, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("chat ai pull error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }


    /**
     * 添加消息
     *
     * @param chatAiRequest
     * @return
     */
    @PostMapping("/crud/chatAi/push")
    public ResponseEntity<?> push(@RequestBody ChatAiRequest chatAiRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        try {
            String toolId = chatAiRequest.getAiToolId();
            String msg = chatAiRequest.getMessage();
            String reasonMessage = chatAiRequest.getReasonMessage();
//            String chatAiId = chatAiRequest.getChatAiId();
            String role = chatAiRequest.getRole();
            String type = chatAiRequest.getType();

            ChatFile file = chatAiRequest.getFile();

            if (!StringUtils.hasLength(msg) && !StringUtils.hasLength(reasonMessage)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            if (!StringUtils.hasLength(toolId) || !StringUtils.hasLength(role) || !StringUtils.hasLength(type)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Date current = new Date();
            ChatAiMessage chatAiMessage = new ChatAiMessage();
//            chatAiMessage.setChatAiId(chatAiId);
            chatAiMessage.setAiToolId(toolId);
            chatAiMessage.setMessage(msg);
            chatAiMessage.setReasonMessage(reasonMessage);
            chatAiMessage.setMessage(msg);
            chatAiMessage.setRole(role);
            chatAiMessage.setType(type);
            chatAiMessage.setUid(loginUser.getUid());
            chatAiMessage.setFile(file);
            chatAiMessage.setCreatedDt(current);
            chatAiMessage.setUpdatedDt(current);

            ChatAiMessage result = chatAiMessageService.add(chatAiMessage);

            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("chat ai push error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }


    /**
     * @param chatAiRequest
     * @return
     */
    @PostMapping("/crud/chatAi/queryMessageList")
    public ResponseEntity<?> queryMessageList(@RequestBody ChatAiRequest chatAiRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            String aiToolId = chatAiRequest.getAiToolId();
            Integer pageNum = chatAiRequest.getPageNum();
            Integer pageSize = chatAiRequest.getPageSize();
            String flagCreatedTime = chatAiRequest.getFlagCreatedTime();
            String message = chatAiRequest.getMessage();
//            String role = chatAiRequest.getRole();
            if (!StringUtils.hasLength(aiToolId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            if (pageNum == null) {
                pageNum = 1;
            }
            if (pageSize == null) {
                pageSize = 20;
            }
            QueryResults result = chatAiMessageService.queryChatMessageByPage(loginUser.getUid(), aiToolId, message, pageNum, pageSize);
//            if (StringUtils.hasLength(flagCreatedTime)) {
//                result = chatAiMessageService.queryMessageByPage(loginUser.getUid(), flagCreatedTime, aiToolId, pageNum, pageSize);
//            } else {
//                result = chatAiMessageService.queryMessageByPage(loginUser.getUid(), aiToolId, pageNum, pageSize);
//            }
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("chat ai queryMessage error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     * @param chatAiId
     * @return
     */
    @DeleteMapping("/crud/chatAi/deleteMessageByChatAiId/{chatAiId}")
    public ResponseEntity<?> deleteMessageByChatAiId(@PathVariable String chatAiId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            if (!StringUtils.hasLength(chatAiId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result2 = chatAiMessageService.removeByChatAiId(loginUser.getUid(), chatAiId);
            return ResponseEntity.ok(new ResultSuccess<>(result2));
        } catch (Exception ex) {
            log.error("deleteCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param id
     * @return
     */
    @DeleteMapping("/crud/chatAi/deleteMessageById/{id}")
    public ResponseEntity<?> deleteMessageById(@PathVariable String id) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            if (!StringUtils.hasLength(id)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result2 = chatAiMessageService.removeById(loginUser.getUid(), id);
            return ResponseEntity.ok(new ResultSuccess<>(result2));
        } catch (Exception ex) {
            log.error("deleteCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
