package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.ChatAiMessage;
import com.seaurl.platform.space.model.QueryResults;
import com.seaurl.platform.space.model.QueryResults2;

import java.util.List;

public interface ChatAiMessageService {

    ChatAiMessage add(ChatAiMessage chatAiMessage);

    List<ChatAiMessage> queryMessage(String uid);

    List<ChatAiMessage> queryMessage(String uid, String type);
    List<ChatAiMessage> queryMessage(String uid, String type, String toolId);

    QueryResults2 queryMessageByPage(String uid, Integer pageNum, Integer pageSize);

    QueryResults queryChatMessageByPage(String uid,String toolId,String message, Integer pageNum, Integer pageSize);

    QueryResults queryChatMessageByPage(String uid,String role,String toolId,String message, Integer pageNum, Integer pageSize);

    QueryResults queryMessageByPage(String uid,String toolId, Integer pageNum, Integer pageSize);
    QueryResults queryMessageByPage(String uid, String datetime, String toolId, Integer pageNum, Integer pageSize);

    boolean removeByChatAiId(String uid, String chatAiId);

    boolean removeById(String uid, String id);
}
