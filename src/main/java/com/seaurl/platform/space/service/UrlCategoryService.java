package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.UrlCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UrlCategoryService {

    boolean dragCategory(String uid,String categoryId, String targetId, String type);

    UrlCategory getMaxSortCategory(String uid, String parentId);

    int getMinSortByCategoryId(String uid, String parentId);

    boolean addCategory(UrlCategory urlCategory);

    UrlCategory addCategory2(UrlCategory urlCategory);

    boolean editCategoryName(String categoryId, String categoryName, String pid);

    boolean deleteCategory(String categoryId);

    void setCategorySortBy(String uid, String categoryId);

    List<UrlCategory> queryCategoryByUserName(String userName);

    UrlCategory queryCategoryById(String id);

    List<UrlCategory> queryCategoryByPid(String pid);

    List<UrlCategory> queryCategoryByUPid(String uid, String pid);

    List<UrlCategory> queryCategoryByUid(String uid);

    List<UrlCategory> queryAllCategoriesByUid(String uid);

    List<HashMap> queryAllCategoriesAndUrlCountByUid(String uid);

    boolean setTop(String cid, Boolean isTop);

    boolean move(String id, String pid, Integer sort);
}
