package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 用户登录会话。原 Redis Hash 存储迁移到 Mongo。
 * id 直接复用 tokenId（UUID），与 UserDevice.tokenId 一一对应。
 * expireAt 为绝对过期时间，业务侧主动判断 + Mongo TTL 索引双保险。
 */
@Document(collection = "userSession")
@Getter
@Setter
public class UserSession {
    @Id
    private String id;
    private String accessToken;
    private String uid;
    private Date expireAt;
    private Date createdDt;
}
