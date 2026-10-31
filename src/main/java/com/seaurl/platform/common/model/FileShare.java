package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class FileShare {
    private String uid;
    private String fileId;
    private List<String> friendIds;
    private Date createdDt;
    private Date updatedDt;
}
