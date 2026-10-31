package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.MailFolder;
import com.seaurl.platform.space.model.MailMessage;
import com.seaurl.platform.common.model.PageResults;
import com.seaurl.platform.space.model.QueryResults;

import java.util.List;

public interface MailMessageService {
    List<MailMessage> addForList(List<MailMessage> mailMessageList);

    List<MailMessage> queryBy(String uid, String email, String folderName);

    Boolean setMessage(String uid, String messageId);

    Boolean removeEmail(String id);

    Boolean setAttachment(String uid, String email, String folderName, Long mid, String fileId, String ossUrl);
    Boolean setAttachment(String uid, String email, String folderName, Long mid, String fileId, String ossUrl, Long fileSize);

    Boolean isExist(String email, Long mid, String folderName);

    QueryResults queryBy(String uid, String email, String folderName, Integer pageNum, Integer pageSize);

    QueryResults queryBy(String uid, String email, String folderName,Boolean isUnread, Integer pageNum, Integer pageSize);
}
