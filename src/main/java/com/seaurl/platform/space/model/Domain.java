package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * 公共图标
 * 作用：防止数据冗余
 */
@Setter
@Getter
public class Domain {

    private String id;

    // 名称 baidu
    private String name;

    // 小图标,如果没获取到则为空
    private String favicon;

    private String lightHexColor;

    private String darkHexColor;

    // 排序
    private Integer sort;

    //是否公开
    private Boolean isPublic;
    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间

    //子域名：api.baidu.com，image.baidu.com
    private List<Domain> subDomains;
}
