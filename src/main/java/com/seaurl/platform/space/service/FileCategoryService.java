package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.FileCategory;
import com.seaurl.platform.common.model.NoteCategory;
import com.seaurl.platform.common.model.UrlCategory;

import java.util.List;

public interface FileCategoryService {
    boolean addCategory(FileCategory fileCategory);

    FileCategory addCategory2(FileCategory fileCategory);

    boolean editCategoryName(String categoryId, String categoryName, String pid);

    boolean deleteCategory(String categoryId);


    List<FileCategory> queryCategoryByUserName(String userName);

    List<FileCategory> queryCategoryByPid(String pid);

    List<FileCategory> queryCategoryByUid(String uid);

    boolean setTop(String cid, Boolean isTop);
}
