package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.File;
import com.seaurl.platform.common.model.Note;
import com.seaurl.platform.common.model.ShareFileToUser;
import com.seaurl.platform.common.model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public interface FileService {

    File add2(File file);

    boolean addToSetFriend(String fileId, ShareFileToUser shareFileToUser);

    boolean deleteFriend(String fileId, String friendId);

    boolean add(File file) throws IOException;

    boolean delete(String fileId, boolean isThoroughly);

    boolean deleteFolder(String fileId, String oldFullPath, String newFullPath, boolean isThoroughly);

    boolean deleteFolder(String fullPath, boolean isThoroughly);

    boolean deleteThoroughly(String fileId);

    boolean putInTrashbin(String fileId);

    boolean takeOutTrashbin(String fileId);

    boolean recovery(String fileId);

    boolean editContent(File file);

    boolean editCategoryId(String fileId, String categoryId);

    boolean move(String fileId, String pid, String fullPath);

    boolean moveFolder(String fileId, String pid, String oldFullPath, String newFullPath);

    boolean recovery(File file);

    boolean recovery(File oldFile, File newFile);

    boolean editIsPrivate(String fileId, Boolean isPrivate);

    boolean editFileName(String fileId, String fileName, String fileUrl, String filePath);

    boolean editFileName(String fileId, String fileName, String fileUrl, String filePath, String fullPath);

    boolean editFolderName(String fileId, String fileName, String oldFullPath, String newFullPath);

    boolean setFavorite(String fileId, Boolean isFavorite);

    boolean updateDt(String fileId);

    File queryFileById(String fileId);

    File queryFileByFullPath(String uid, String fullPath);

    boolean searchFolderPathIsExistFile(String uid, String folderPrevPath, String fileName, boolean isFolder);

    boolean isExistFullPath(String uid, String fullPath);

    boolean isExistShareFriend(String uid, String fileId, String friendId);

    List<File> queryFileByPid(String pid);

    List<File> queryFileByPid(String pid, String uid);

    List<File> queryFileListByFullPath(String uid, String fullPath);

    List<File> queryFileListByFullPath(String uid, String fullPath, Boolean isDelete);

    List<File> queryFileListByType(String uid, String type);

    List<File> queryFileListByFolder(String uid);

    List<File> queryFileListByTwoWeeks(String uid, String fullPath);

    List<File> queryFileListByFavorites(String uid);

    List<File> queryFileListByFavorites(String uid, String fullPath);

    List<File> queryDeleteFileListByOneMonth(String uid);

    List<File> queryDeleteFileListByOneMonth(String uid, String fullPath);

    List<File> queryFileByTitle(String uid, String title);

    boolean findFileAndDelete(String uid, String fileName);

    File getFileSizeByUserId(String uid);


    List<File> queryFolderList(String uid);

    List<File> queryFolderList(String uid, List<String> fileIds);

    List<File> queryMyShareFileList(String uid);

    List<File> queryShareFileToMe(String uid);
}
