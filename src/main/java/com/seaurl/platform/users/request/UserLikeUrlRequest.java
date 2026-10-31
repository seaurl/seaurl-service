package com.seaurl.platform.users.request;

import lombok.Data;

@Data
public class UserLikeUrlRequest {
    private String[] urlIds;

    private String urlId;

    private String byUid;
}
