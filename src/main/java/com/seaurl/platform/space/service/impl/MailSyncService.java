package com.seaurl.platform.space.service.impl;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.platform.space.model.MailAttachment;
import com.seaurl.platform.space.model.MailFolder;
import com.seaurl.platform.space.model.MailMessage;
import com.seaurl.platform.space.model.MailProvider;
import com.seaurl.platform.space.service.MailMessageService;
import com.seaurl.platform.space.service.MailProviderService;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class MailSyncService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MailMessageService mailMessageService;

    @Autowired
    private MailProviderService mailProviderService;

    /**
     * 同步指定邮箱的邮件
     *
     * @param mailProvider 邮箱提供商配置
     */
    public void syncMail(MailProvider mailProvider) {
        try {
            String uid = mailProvider.getUid();
            String email = mailProvider.getEmail();

            if (!StringUtils.hasLength(uid) || !StringUtils.hasLength(email)) {
                log.info("uid或email为空");
                mailProviderService.updateStatus(email, uid, "-1");
                return;
            }

            try {
                if (mailProvider == null) {
                    log.info("mailProvider为空");
                    mailProviderService.updateStatus(email, uid, "-1");
                    return;
                }
                log.info("mailProvider=：{}", mailProvider);
                Properties properties = new Properties();
                if (mailProvider.getProtocol().equals("imaps")) {
                    properties.put("mail.imap.host", mailProvider.getImapHost());
                    properties.put("mail.imap.port", mailProvider.getImapPort());
                    properties.put("mail.imap.ssl.enable", mailProvider.getImapSslEnable());  // 启用SSL
                }
                if (mailProvider.getProtocol().equals("pop3s")) {
                    properties.put("mail.pop3.host", mailProvider.getPopHost());
                    properties.put("mail.pop3.port", mailProvider.getPopPort());
                    properties.put("mail.pop3.ssl.enable", mailProvider.getPopSslEnable());  // 启用SSL
                }

                log.info("======getDefaultFolder========{}", properties);
                Session session = Session.getDefaultInstance(properties);
                IMAPStore store = (IMAPStore) session.getStore(mailProvider.getProtocol());

                store.connect(mailProvider.getImapHost(), mailProvider.getEmail(), mailProvider.getPassword()); // 使用授权码而不是密码

                properties.put("mail.store.protocol", mailProvider.getProtocol());


                if (store.getDefaultFolder() == null || store.getDefaultFolder().list().length == 0) {
                    log.info("没有查询到文件夹！");
                    mailProviderService.updateStatus(email, uid, "-1");
                    return;
                }

                //1、修改同步状态为：正在同步邮件
                List<MailFolder> mailFolderList = mailProvider.getMailFolderList();
                for (MailFolder mailFolder : mailFolderList) {
                    log.info("--- next:folderName:{},email:{}", mailFolder.getRealName(), mailFolder.getEmail());
                    // 加这个不会报错：Unsafe Login. Please contact kefu@188.com for help
                    if (mailProvider.getProvider().equals("126")) {
                        HashMap IAM = new HashMap();
                        //带上IMAP ID信息，由key和value组成，例如name，version，vendor，support-email等。
                        IAM.put("name", "seaurl email client");
                        IAM.put("version", "1.0.0");
                        IAM.put("vendor", "seaurl");
                        IAM.put("support-email", "zhangwei@seaurl.com");
                        store.id(IAM);
                    }

                    try {
                        IMAPFolder folder = (IMAPFolder) store.getFolder(mailFolder.getRealName());
                        folder.open(Folder.READ_ONLY);
                        int totalMessages = folder.getMessageCount();
                        if (totalMessages == 0) {
                            log.info("文件夹没有邮件");
                            continue;
                        }
                        log.info("totalMessages ={}", totalMessages);
                        jakarta.mail.Message[] messages;

                        if (mailProvider.getLastSync() != null) { //第2次及之后使用增量同步邮件
                            // 大于（Greater Than）
                            // 如果发送邮件之后立即获取就会出现获取不到邮件的情况，
                            // 解决：最后一次同步时间向前推个5分钟，
                            // 往前推5分钟
                            LocalDateTime lastSync = mailProvider.getLastSync().toInstant()
                                    .atZone(ZoneId.of("UTC"))
                                    .toLocalDateTime();

                            // 加上8小时，再往前推迟1天
                            LocalDateTime adjustedTime = lastSync.plusHours(8).minusDays(1);

                            // 转换回 Date
                            Date adjustedDate = Date.from(adjustedTime.atZone(ZoneId.systemDefault()).toInstant());
                            // 有时获取不到最新邮件？后面如果还有问题再看看
                            ReceivedDateTerm searchTerm = new ReceivedDateTerm(ComparisonTerm.GE, adjustedDate);
                            messages = folder.search(searchTerm);
                            log.info("--- 增量同步邮件 ---{}", messages.length);
                        } else { //第1次收取邮件
                            messages = folder.getMessages();
                            log.info("--- 收取全部邮件 ---{}", messages.length);
                        }

                        List<MailMessage> mailMessageList = new ArrayList<>();

                        Date date = new Date();
                        for (jakarta.mail.Message mailMessage : messages) {
                            log.info("正在添加新邮件");
                            // 获取邮件的 UID
                            long mid = folder.getUID(mailMessage);

                            // 注意：查询表中是否已经存在该邮件，如果存在就不用再保存了
                            Boolean isExist = mailMessageService.isExist(email, mid, mailFolder.getRealName());
                            log.info("---isExist:{}", isExist);
                            if (!isExist) {
                                log.info("UID: {}", mid);
                                // 判断未读状态
                                boolean isUnread = !mailMessage.isSet(Flags.Flag.SEEN);
                                boolean isFlag = mailMessage.isSet(Flags.Flag.FLAGGED);

                                MailMessage mailMessageModel = new MailMessage();
                                mailMessageModel.setUid(uid);
                                mailMessageModel.setEmail(email);
                                mailMessageModel.setMid(mid);
                                mailMessageModel.setFolderName(mailFolder.getRealName());
                                mailMessageModel.setContent(getTextFromMessage(mailMessage));

                                mailMessageModel.setFrom(getAddressList(mailMessage.getFrom()));
                                mailMessageModel.setReplyTo(getAddressList(mailMessage.getReplyTo()));

                                mailMessageModel.setRecipients(getAddressList(mailMessage.getRecipients(jakarta.mail.Message.RecipientType.TO)));

                                mailMessageModel.setSubject(mailMessage.getSubject());
                                mailMessageModel.setIsFlagged(isFlag);
                                mailMessageModel.setIsUnread(isUnread);
                                mailMessageModel.setCount(mailMessage.getMessageNumber());
                                mailMessageModel.setSentDate(mailMessage.getSentDate());
                                mailMessageModel.setReceivedDate(mailMessage.getReceivedDate());
                                mailMessageModel.setAttachments(getAttachmentsFromMessage(mailMessage));
                                mailMessageModel.setIsDelete(false);
                                mailMessageModel.setCreatedDt(date);
                                mailMessageModel.setUpdatedDt(date);

                                mailMessageList.add(mailMessageModel);
                            } else {
                                log.info("已存在mid={}相同邮件，不做处理", mid);
                            }
                        }
                        log.info("--- mailMessageList size ---{}", mailMessageList.size());
                        // 列表不为空，则保存到数据库
                        if (!mailMessageList.isEmpty()) {
                            log.info("开始添加邮件到数据库中");
                            mailMessageService.addForList(mailMessageList);
                            log.info("添加邮件成功");
                        }

                    } catch (Exception ex) {
                        log.error("查询文件夹或者保存出错：{}", ex.getMessage());
                    }
                }
                log.info("--- 获取全部邮件成功 ---");
                mailProviderService.updateStatus(email, uid, "1");
            } catch (Exception ex) {
                mailProviderService.updateStatus(email, uid, "-1");
                log.error("同步邮件 mailProvider 出错：{}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("同步邮件任务异常: ", e);
        }
    }

    public List<HashMap> getAddressList(Address[] addresses) {
        List<HashMap> list = new ArrayList<>();
        if (addresses != null) {
            for (Address address : addresses) {
                if (address instanceof InternetAddress) {
                    String emailAddress = ((InternetAddress) address).getAddress();
                    String personal = ((InternetAddress) address).getPersonal();
                    if (personal != null) {
                        personal = personal.replaceAll("^\"|\"$", ""); // 去掉多余的双引号
                    }
                    if (emailAddress != null && !emailAddress.isEmpty()) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("email", emailAddress);
                        map.put("personal", personal);
                        list.add(map);
                    }
                } else {
                    log.warn("Address is not an instance of InternetAddress: {}", address);
                }
            }
        } else {
            log.warn("Addresses array is null");
        }
        return list;
    }

    public String getTextFromMessage(jakarta.mail.Message message) throws MessagingException, IOException {
        try {
            StringBuilder plainText = new StringBuilder();
            StringBuilder htmlText = new StringBuilder();
            Map<String, DataSource> cidToDataSourceMap = new HashMap<>(); // 用于存储 cid 到附件数据源的映射

            if (message.isMimeType("text/plain")) {
                return message.getContent().toString();
            } else if (message.isMimeType("multipart/*") && message.getContent() instanceof MimeMultipart mimeMultipart) {
                return extractTextFromMimeMultipart(plainText, htmlText, cidToDataSourceMap, mimeMultipart);
            } else if (message.isMimeType("text/html")) {
                return message.getContent().toString();
            } else if (message.isMimeType("message/rfc822") && message.getContent() instanceof jakarta.mail.Message nestedMessage) {
                return getTextFromMessage(nestedMessage);
            }
        } catch (MessagingException | IOException e) {
            log.error("解析邮件时出错，邮件内容类型: {}，错误信息: {}", message.getContentType(), e.getMessage());
        }
        return "";
    }

    private String extractTextFromMimeMultipart(StringBuilder plainText, StringBuilder htmlText, Map<String, DataSource> cidToDataSourceMap, MimeMultipart mimeMultipart) throws MessagingException, IOException {
        try {
            int count = mimeMultipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);

                if (bodyPart.isMimeType("text/plain")) {
                    String content = bodyPart.getContent().toString();
                    if (!plainText.toString().contains(content)) { // 注意：避免重复添加
                        plainText.append(content);
                    }
                } else if (bodyPart.isMimeType("text/html")) {
                    String content = bodyPart.getContent().toString();
                    if (!htmlText.toString().contains(content)) { // 避免重复添加
                        htmlText.append(content);
                    }
                } else if (bodyPart.isMimeType("multipart/*") && bodyPart.getContent() instanceof MimeMultipart subMultipart) {
                    String nestedContent = extractTextFromMimeMultipart(plainText, htmlText, cidToDataSourceMap, subMultipart);
                    if (!htmlText.isEmpty()) {
                        if (!htmlText.toString().contains(nestedContent)) { // 注意：避免重复添加
                            htmlText.append(nestedContent);
                        }
                    } else {
                        if (!plainText.toString().contains(nestedContent)) { // 注意：避免重复添加
                            plainText.append(nestedContent);
                        }
                    }
                } else if (bodyPart.isMimeType("message/rfc822") && bodyPart.getContent() instanceof jakarta.mail.Message nestedMessage) {
                    String nestedContent = getTextFromMessage(nestedMessage);
                    if (!plainText.toString().contains(nestedContent)) { // 避免重复添加
                        plainText.append(nestedContent);
                    }
                } else if (bodyPart.isMimeType("image/*")) { //获取邮件正文中的图片，如果包含cid则获取图片流以base64形式放入到src中
                    // 提取嵌入的图片
                    String contentId = bodyPart.getHeader("Content-ID") != null
                            ? bodyPart.getHeader("Content-ID")[0].replaceAll("<", "").replaceAll(">", "")
                            : null;
                    if (contentId != null) {
                        DataSource dataSource = bodyPart.getDataHandler().getDataSource();
                        cidToDataSourceMap.put(contentId, dataSource);
                    }
                }


            }
            // 原因：邮件正文图片src中是cid不是真实的图片路径，所以替换 HTML 中的 cid 引用为 Base64 数据来展示图片
            if (!cidToDataSourceMap.isEmpty()) {
                replaceCidReferences(htmlText, cidToDataSourceMap);
            }
        } catch (Exception ex) {
            log.error("extractTextFromMimeMultipart 错误信息: {}", ex.getMessage());
        }
        return !htmlText.isEmpty() ? htmlText.toString() : plainText.toString();
    }

    public List<MailAttachment> getAttachmentsFromMessage(jakarta.mail.Message message) throws MessagingException, IOException {
        List<MailAttachment> attachments = new ArrayList<>();
        try {
            if (message.isMimeType("multipart/*") && message.getContent() instanceof MimeMultipart mimeMultipart) {
                extractAttachmentsFromMimeMultipart(mimeMultipart, attachments);
            }
        } catch (MessagingException | IOException e) {
            log.error("提取附件时出错，邮件内容类型: {}，错误信息: {}", message.getContentType(), e.getMessage());
        }
        return attachments;
    }

    private void extractAttachmentsFromMimeMultipart(MimeMultipart mimeMultipart, List<MailAttachment> attachments) throws MessagingException, IOException {
        try {
            int count = mimeMultipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);

                if (bodyPart.isMimeType("multipart/related")) {
                    // 跳过嵌入资源部分
                    continue;
                }

                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                        (Part.INLINE.equalsIgnoreCase(bodyPart.getDisposition()) && bodyPart.getHeader("Content-ID") == null)) {
                    // 是附件或者没有 Content-ID 的 inline
                    String fileName = bodyPart.getFileName();

                    // 检查文件名是否为 null
                    if (fileName != null) {
                        fileName = MimeUtility.decodeText(fileName);

                        if (fileName == null || fileName.matches("image\\d+\\..*")) {
                            // 跳过命名为 imageXXX 的部分
                            continue;
                        }

                        String fileId = UUID.randomUUID().toString();
                        attachments.add(new MailAttachment(fileId, fileName, bodyPart.getContentType(), (long) bodyPart.getSize()));
                    }
                } else if (bodyPart.isMimeType("multipart/*") && bodyPart.getContent() instanceof MimeMultipart subMultipart) {
                    extractAttachmentsFromMimeMultipart(subMultipart, attachments);
                }
            }
        } catch (Exception ex) {
            log.error("extractAttachmentsFromMimeMultipart错误信息: {}", ex.getMessage());
        }
    }

    // 替换邮件正文中的 cid 引用为 Base64 编码的图像数据
    private void replaceCidReferences(StringBuilder htmlText, Map<String, DataSource> cidToDataSourceMap) throws IOException {
        log.info("HTML Text Before Processing: {}", htmlText);
        try {
            for (Map.Entry<String, DataSource> entry : cidToDataSourceMap.entrySet()) {
                String cid = entry.getKey();
                log.info("cid={}", cid);
                DataSource dataSource = entry.getValue();
                // 将图片数据转换为 Base64 编码
                try (InputStream inputStream = dataSource.getInputStream()) {
                    byte[] imageBytes = inputStream.readAllBytes();
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    // 提取 MIME 类型
                    String contentType = dataSource.getContentType();
                    String mimeType = contentType.split(";")[0].trim();
                    String dataUri = "data:" + mimeType + ";base64," + base64Image;
                    // 替换 HTML 中的 cid 引用
                    String cidReference = "cid:" + cid;
                    int index = htmlText.indexOf(cidReference);
                    if (index != -1) {
                        htmlText.replace(index, index + cidReference.length(), dataUri);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("replaceCidReferences错误信息: {}", ex.getMessage());
        }
    }
}
