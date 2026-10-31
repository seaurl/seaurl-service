package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.File;
import com.seaurl.platform.common.model.FileShare;
import com.seaurl.platform.common.model.User;

import java.util.HashMap;
import java.util.List;

public interface FileShareService {
    FileShare add(FileShare fileShare);

    boolean isExist(String uid, String fileId);

    boolean update(FileShare fileShare);

    boolean deleteFriend(String uid, String fileId,String fid);

    List<User> queryShareFileFriends(String uid, String fileId);

    FileShare queryBy(String uid, String fileId);

    List<HashMap> queryMyShareFileList(String myUid);
}
