package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.UrlCategory;

import java.util.List;

public interface UrlCategoryService {
    boolean addCategory(UrlCategory urlCategory);

    boolean addSubCategory(String uid, String pid, String name);

    boolean editCategoryName(String categoryId, String categoryName);

    boolean deleteCategory(String categoryId);

    List<UrlCategory> queryCategoryByUserName(String userName);

    List<UrlCategory> queryCategoryByPid(String pid);

    List<UrlCategory> queryCategoryByUid(String uid);
}
