package com.seaurl.platform.space.model;

import jakarta.mail.Message;
import lombok.Data;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPStore;

@Data
public class ActionMailResult {

    IMAPStore store;

    IMAPFolder folder;

    Message message;

    public ActionMailResult(IMAPStore imapStore, IMAPFolder imapFolder, Message mailMessage){
        store = imapStore;
        folder = imapFolder;
        message = mailMessage;
    }
}
