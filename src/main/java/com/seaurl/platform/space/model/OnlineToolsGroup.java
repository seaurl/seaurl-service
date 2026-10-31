package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class OnlineToolsGroup {
    private String id;

    private String group;

    private String groupName;

    private Integer sort;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
