package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.AiConfig;
import com.seaurl.platform.space.model.AiMessage;

import java.util.List;

public interface AiService {
    AiConfig getConfig(String uid);

    AiConfig saveConfig(String uid, String model, String apiKey, String baseUrl);

    AiMessage saveMessage(String uid, String conversationId, String role, String content, String title);

    List<AiMessage> getMessages(String uid, String conversationId);

    List<String> getConversations(String uid);

    boolean deleteConversation(String uid, String conversationId);

    boolean deleteAllConversations(String uid);
}
