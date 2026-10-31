package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.NoteCategory;
import com.seaurl.platform.common.model.UrlCategory;

import java.util.HashMap;
import java.util.List;

public interface NoteCategoryService {

    boolean dragCategory(String uid,String categoryId, String targetId, String type);

    NoteCategory getMaxSortCategory(String uid, String parentId);

    int getMinSortByCategoryId(String uid, String parentId);

    boolean addCategory(NoteCategory noteCategory);

    NoteCategory addCategory2(NoteCategory noteCategory);

    boolean editCategoryName(String categoryId, String categoryName, String pid);

    boolean deleteCategory(String categoryId);

    void setCategorySortBy(String uid, String categoryId);

    List<NoteCategory> queryCategoryByUserName(String userName);

    List<NoteCategory> queryCategoryByPid(String pid);

    List<NoteCategory> queryCategoryByUid(String uid);

    List<HashMap> queryAllCategoriesAndNoteCountByUid(String uid);

    boolean setTop(String cid, Boolean isTop);
}
