package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class CommonUrlsGroup {
    private String id;

    private String group;

    private String groupName;

    private List<CommonUrls> commonUrlsList;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
