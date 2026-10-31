package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.PasswordCategory;
import com.seaurl.platform.space.model.PasswordEntry;

import java.util.List;

public interface PasswordService {
    List<PasswordEntry> queryAll(String uid);

    List<PasswordEntry> search(String uid, String keyword);

    PasswordEntry add(String uid, String title, String username, String password, String url, String repoUrl, String repoBranch, String notes, String category, String parentId, String color, String categoryId);

    boolean update(String id, String title, String username, String password, String url, String repoUrl, String repoBranch, String notes, String category, String parentId, String color, String categoryId);

    boolean delete(String id);

    /**
     * 查询当前用户的全部分类（含 id/name/parentId，供前端构树）
     */
    List<PasswordCategory> categories(String uid);

    PasswordCategory addCategory(String uid, String name, String parentId);

    /**
     * 按 id 重命名分类（账号靠 categoryId 关联，重命名无需同步账号）
     */
    boolean renameCategory(String uid, String id, String name);

    /**
     * 查询某父分类下的直接子分类
     */
    List<PasswordCategory> queryCategoryByPid(String uid, String pid);

    /**
     * 查询挂在某分类下的账号
     */
    List<PasswordEntry> queryEntryByCategoryId(String uid, String categoryId);

    boolean deleteCategory(String id);

    /**
     * 移动分类到新的父分类下（parentId 为空表示移到顶级）。
     * 防环：不允许移到自身或其子分类下。返回错误提示，null 表示成功。
     */
    String moveCategory(String uid, String id, String parentId);

    /**
     * 一次性迁移：把老数据 entry.category(名字) 解析回填为 categoryId。幂等。
     */
    int migrate();
}
