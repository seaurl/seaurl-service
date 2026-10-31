package com.seaurl.platform.space.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "password_category")
public class PasswordCategory {
    @Id
    private String id;

    private String uid;

    private String name;

    /**
     * 父分类 ID，null 或空表示根分类
     */
    private String parentId;

    private Date createdDt;

    private Date updatedDt;
}
