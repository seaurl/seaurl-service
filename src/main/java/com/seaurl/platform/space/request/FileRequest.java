package com.seaurl.platform.space.request;

import com.seaurl.platform.common.model.File;
import com.seaurl.platform.common.model.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class FileRequest {
    // 文件id
    private String[] fileIds;

    private String fileId;

    private String fileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    private String fileContentType;

    private Boolean isPrivate = false;

    // 分类id
    private String pid;

    private String imageAiId;

    private String qrcodeImg;

//    private String excaliDrawJson;

    private String fileContent;

    private String fullPath;

    List<File> files;

    private String downloadFileName;

    private String deleteType;

    private String fid;

    private List<String> fids;

    private User user;
}
