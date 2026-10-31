package com.seaurl.platform.space.service;


import com.seaurl.platform.space.model.ChatAiCategory;

import java.util.List;

public interface ChatAiCategoryService {
    boolean add(ChatAiCategory chatAiCategory);
    ChatAiCategory add2(ChatAiCategory chatAiCategory);

    boolean edit(ChatAiCategory chatAiCategory);

    boolean setTop(ChatAiCategory chatAiCategory);

    boolean remove(String uid, String chatAiId);

    List<ChatAiCategory> queryByUid(String uid);
}
