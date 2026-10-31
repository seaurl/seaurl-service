package com.seaurl.platform.space.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "password_entry")
public class PasswordEntry {
    @Id
    private String id;

    private String uid;

    private String title;

    private String username;

    /**
     * AES-256-GCM 加密后的密码
     */
    private String passwordEncrypted;

    /**
     * 初始化向量 (Base64)
     */
    private String iv;

    /**
     * GCM 认证标签 (Base64)
     */
    private String authTag;

    private String url;

    private String repoUrl;

    private String repoBranch;

    private String notes;

    private String category;

    /**
     * 分类 ID（多级分类改造后关联 PasswordCategory.id；老数据迁移后回填）
     */
    private String categoryId;

    /**
     * 父账号 ID，null 表示主账号
     */
    private String parentId;

    /**
     * 卡片显示颜色（HEX），默认 #52c41a
     */
    private String color;

    private Date createdDt;

    private Date updatedDt;
}
