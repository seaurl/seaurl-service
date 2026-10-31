package com.seaurl.platform.space.service;


import com.seaurl.platform.space.model.KnowledgeCategory;

import java.util.List;

public interface KnowledgeCategoryService {
    KnowledgeCategory create(KnowledgeCategory category);
    KnowledgeCategory getById(String id, String uid);
    List<KnowledgeCategory> getAll(String uid);
    Boolean update(KnowledgeCategory category);
    Boolean delete(String id);
    List<KnowledgeCategory> findAllByOrderBySortAsc();
}
