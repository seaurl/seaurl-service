package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class File implements Cloneable {
    private String id;
    private String uid;
    private String pid;
    private String name;
    private String type;
    private Long size;
    private String url;
    private String path;
    private String fullPath;
    private String encodeFullPath;
    private Boolean isPrivate;
    private Boolean isFavorite;
    private Boolean isTrashbin;
    private Boolean isDelete;
    private List<ShareFileToUser> shareToFriends;
    private Date createdDt;
    private Date updatedDt;

    @Override
    public File clone() {
        try {
            return (File) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
