package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class MostUrlsRank {
    private String domain;

    //如果没获取前端显示domain首字母
    private String favicon;

    private Integer count;


}
