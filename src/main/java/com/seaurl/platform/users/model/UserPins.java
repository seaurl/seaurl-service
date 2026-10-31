package com.seaurl.platform.users.model;

import com.seaurl.platform.common.model.Url;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class UserPins {
    //  id
    private String id;

    private String uid;

    private List<Url> urlList;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
