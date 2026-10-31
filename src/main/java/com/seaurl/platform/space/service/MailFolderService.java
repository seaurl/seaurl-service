package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.MailFolder;

import java.util.List;

public interface MailFolderService {

    void  add(MailFolder mailFolder);

    List<MailFolder>  addForList(List<MailFolder> mailFolder);

    List<MailFolder> queryFolder(String email);
}
