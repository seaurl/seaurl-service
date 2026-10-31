package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {
    private String userName;
    private String[] urlIds;
    private String urlId;
    private String byUid;
}
