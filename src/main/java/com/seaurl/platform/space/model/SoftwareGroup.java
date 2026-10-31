package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SoftwareGroup {
    private String id;

    // 英文名
    private String group;

    // 中文名
    private String groupName;

    private String icon;

    private Integer sort;

    private List<SoftwareGroup> softwareGroupList;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
