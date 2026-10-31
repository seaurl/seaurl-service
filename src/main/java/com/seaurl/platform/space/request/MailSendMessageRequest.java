package com.seaurl.platform.space.request;

import com.seaurl.platform.common.model.File;
import com.seaurl.platform.space.model.MailAttachment;
import jakarta.mail.Address;
import jakarta.mail.internet.InternetAddress;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class MailSendMessageRequest {
    // 收件人
    private InternetAddress[] recipients;

    // 抄送人
    private InternetAddress[] ccRecipients;

    // 回复人
    private InternetAddress[] replyTo;

    // 发件人
    private InternetAddress[] from;

    // 我的文件中选择的文件
    private List<File> myFiles;

    // 原有附件列表
    private List<File> originFiles;

    // 主题
    private String subject;

    // 邮件正文
    private String content;

    // 文件夹名称
    private String folderName;

    // 邮件id
    private Long mid;
}
