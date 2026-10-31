package com.seaurl.platform.space.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.common.model.File;
import com.seaurl.platform.space.request.CategoryRequest;
import com.seaurl.platform.space.request.MailMessageRequest;
import com.seaurl.platform.space.request.MailProviderRequest;
import com.seaurl.platform.space.request.MailSendMessageRequest;
import com.seaurl.platform.space.service.*;
import com.seaurl.platform.space.service.impl.MailSyncService;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.URLDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.bson.types.ObjectId;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPMessage;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/space")

public class MailController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("mailSyncExecutor")
    private TaskExecutor mailSyncExecutor;

    @Autowired
    MailProviderService mailProviderService;

    @Autowired
    MailFolderService mailFolderService;

    @Autowired
    MailMessageService mailMessageService;

    @Autowired
    FileService fileService;

    @Autowired
    StorageService storageService;

    @Autowired
    AliyunConfig aliyunOssConfig;

    @Autowired
    MailSyncService mailSyncService;

    public static String getDomain(String email) {
        int atIndex = email.indexOf('@');
        int dotIndex = email.indexOf('.', atIndex);  // 找到 @ 之后的第一个 .

        if (atIndex != -1 && dotIndex != -1) {
            return email.substring(atIndex + 1, dotIndex);  // 提取 @ 和 . 之间的部分
        }
        return null;
    }

    /**
     * @param mailProviderRequest //            imap，不加密143，SSL993,TLS
     *                            //            pop，不加密110，SSL995,TLS
     *                            //            smtp，不加密25，SSL465,TLS587
     * @return
     */
    @PostMapping("/crud/mail/addProvider")
    public ResponseEntity<?> addMailProvider(@RequestBody MailProviderRequest mailProviderRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            log.info("--- mailProviderRequest ---{}", JSONArray.toJSONString(mailProviderRequest));

            String name = mailProviderRequest.getName();
            String email = mailProviderRequest.getEmail();
            String password = mailProviderRequest.getPassword();
            String protocol = mailProviderRequest.getProtocol();
            String provider = mailProviderRequest.getProvider();

            String imapHost = mailProviderRequest.getImapHost();
            String imapPort = mailProviderRequest.getImapPort();
            Boolean imapSslEnable = mailProviderRequest.getImapSslEnable();

            String popHost = mailProviderRequest.getPopHost();
            String popPort = mailProviderRequest.getPopPort();
            Boolean popSslEnable = mailProviderRequest.getPopSslEnable();

            String smtpHost = mailProviderRequest.getSmtpHost();
            String smtpPort = mailProviderRequest.getSmtpPort();
            Boolean smtpSslEnable = mailProviderRequest.getSmtpSslEnable();

            if (!StringUtils.hasLength(name) || !StringUtils.hasLength(email) || !StringUtils.hasLength(password) || !StringUtils.hasLength(protocol) || !StringUtils.hasLength(provider)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            boolean existEmail = mailProviderService.isExistEmail(loginUser.getUid(), email);
            if (existEmail) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_EXIST));
            }

            boolean existName = mailProviderService.isExistName(loginUser.getUid(), name);
            if (existName) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.SAME_NAME));
            }

            if (!StringUtils.hasLength(imapHost) && !StringUtils.hasLength(popHost)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            if (!StringUtils.hasLength(imapPort) && !StringUtils.hasLength(popPort)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            if (!StringUtils.hasLength(smtpHost) || !StringUtils.hasLength(smtpPort)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            Date date = new Date();
            MailProvider mailProvider = new MailProvider();

            mailProvider.setUid(loginUser.getUid());

            mailProvider.setName(name);
            mailProvider.setEmail(email);
            mailProvider.setPassword(password);
            mailProvider.setProtocol(protocol);
            mailProvider.setProvider(provider);

            mailProvider.setSmtpHost(smtpHost);
            mailProvider.setSmtpPort(smtpPort);
            mailProvider.setSmtpSslEnable(smtpSslEnable);
            // 设置状态为正在同步
            mailProvider.setStatus("2");

            mailProvider.setCreatedDt(date);
            mailProvider.setUpdatedDt(date);
            Properties properties = new Properties();
            if (mailProvider.getProtocol().equals("imaps")) {
                mailProvider.setImapHost(imapHost);
                mailProvider.setImapPort(imapPort);
                mailProvider.setImapSslEnable(imapSslEnable);

                properties.put("mail.imap.host", mailProvider.getImapHost());
                properties.put("mail.imap.port", mailProvider.getImapPort());
                properties.put("mail.imap.ssl.enable", mailProvider.getImapSslEnable());  // 启用SSL
            }
            if (mailProvider.getProtocol().equals("pop3s")) {
                mailProvider.setPopHost(popHost);
                mailProvider.setPopPort(popPort);
                mailProvider.setPopSslEnable(popSslEnable);

                properties.put("mail.pop3.host", mailProvider.getPopHost());
                properties.put("mail.pop3.port", mailProvider.getPopPort());
                properties.put("mail.pop3.ssl.enable", mailProvider.getPopSslEnable());  // 启用SSL
            }

            properties.put("mail.store.protocol", mailProvider.getProtocol());
            log.info("======getDefaultFolder========{}", properties);
            Session session = Session.getDefaultInstance(properties);
            Store store = session.getStore(mailProvider.getProtocol());

            store.connect(mailProvider.getImapHost(), mailProvider.getEmail(), mailProvider.getPassword()); // 使用授权码而不是密码
            if (store.getDefaultFolder() == null || store.getDefaultFolder().list().length == 0) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.ADD_ERROR));
            }
            Folder[] folders = store.getDefaultFolder().list();

            List<MailFolder> mailFolderList = new ArrayList<>();
            for (Folder folder : folders) {
                log.error("=== folder={}", folder.getFullName());
                MailFolder mailFolder = new MailFolder();
                ObjectId folderId = new ObjectId();
                mailFolder.setId(folderId.toString());
                mailFolder.setUid(loginUser.getUid());
                mailFolder.setEmail(email);
                mailFolder.setRealName(folder.getFullName());

                mailFolder.setName(folder.getFullName());
                if (folder.getFullName().equalsIgnoreCase("inbox")) {
                    mailFolder.setName("收件箱");
                } else if (folder.getFullName().equalsIgnoreCase("sent messages")) {
                    mailFolder.setName("已发送");
                } else if (folder.getFullName().equalsIgnoreCase("drafts")) {
                    mailFolder.setName("草稿箱");
                } else if (folder.getFullName().equalsIgnoreCase("deleted messages")) {
                    mailFolder.setName("已删除");
                } else if (folder.getFullName().equalsIgnoreCase("junk")) {
                    mailFolder.setName("垃圾邮件");
                }

                switch (mailFolder.getName()) {
                    case "收件箱":
                        mailFolder.setSort(1);
                        break;
                    case "未读邮件":
                        mailFolder.setSort(2);
                        break;
                    case "红旗邮件":
                        mailFolder.setSort(3);
                        break;
                    case "草稿箱":
                        mailFolder.setSort(4);
                        break;
                    case "已发送":
                        mailFolder.setSort(5);
                        break;
                    case "已删除":
                        mailFolder.setSort(6);
                        break;
                    case "垃圾邮件":
                        mailFolder.setSort(7);
                        break;
                    case "病毒邮件":
                        mailFolder.setSort(8);
                        break;
                    case "广告邮件":
                        mailFolder.setSort(9);
                        break;
                    case "订阅邮件":
                        mailFolder.setSort(10);
                        break;
                    case "Deleted Items":
                        mailFolder.setSort(11);
                        break;
                    default:
                        mailFolder.setSort(100);
                        break;
                }

                mailFolder.setDeletedMessageCount(folder.getDeletedMessageCount());
                mailFolder.setNewMessageCount(folder.getNewMessageCount());
                mailFolder.setMessageCount(folder.getMessageCount());
                mailFolder.setUnreadMessageCount(folder.getUnreadMessageCount());
                mailFolder.setCreatedDt(date);
                mailFolder.setUpdatedDt(date);

                mailFolderList.add(mailFolder);
            }
            store.close();
            if (!mailFolderList.isEmpty()) {

                // 给文件夹排序
                mailFolderList.sort(Comparator.comparingInt(MailFolder::getSort));
                mailProvider.setMailFolderList(mailFolderList);
                MailProvider result = mailProviderService.add(mailProvider);

                return ResponseEntity.ok(new ResultSuccess<>(result));
            }
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.ADD_ERROR));
        } catch (Exception ex) {
            log.error("updateTop exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 重新同步
     * 1、修改provider status = 2正在同步中
     * 2、发送同步消息
     *
     * @return
     */
    @PostMapping("/crud/mail/resync")
    public ResponseEntity<?> resyncMail(@RequestBody MailProviderRequest mailProviderRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String mail = mailProviderRequest.getEmail();
        if (!StringUtils.hasLength(mail)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }


        try {
            MailProvider mailProvider = mailProviderService.queryBy(mail, loginUser.getUid());

            if (mailProvider != null) {

                Boolean r = mailProviderService.updateStatus(mail, loginUser.getUid(), "2");
                if (r) {
                    // 异步执行邮件同步：用 Spring 管理的专用线程池，worker 线程持有应用类加载器，
                    // 避免 ForkJoinPool.commonPool 的 TCCL 丢失导致 activation SPI 查找失败
                    mailSyncExecutor.execute(() -> mailSyncService.syncMail(mailProvider));
                    return ResponseEntity.ok(new ResultSuccess<>(true));
                } else {
                    return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UPDATE_ERROR));
                }
            } else {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.QUERY_EMPTY));
            }
        } catch (Exception ex) {
            log.error("resyncMail exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取邮箱列表
     *
     * @return
     */
    @GetMapping("/crud/mail/getProvider")
    public ResponseEntity<?> getProvider() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<MailProvider> result = mailProviderService.queryAll(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getProvider exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 修改名称和昵称
     *
     * @param mailMessageRequest
     * @return
     */
    @PostMapping("/crud/mail/updateProvider")
    public ResponseEntity<?> updateProvider(@RequestBody MailMessageRequest mailMessageRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            MailProvider mailProvider = new MailProvider();
            mailProvider.setEmail(mailMessageRequest.getEmail());
            mailProvider.setName(mailMessageRequest.getName());
            mailProvider.setNickName(mailMessageRequest.getNickName());
            Boolean result = mailProviderService.update(mailProvider);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getProvider exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除邮箱
     *
     * @return
     */
    @DeleteMapping("/crud/mail/deleteProvider/{id}")
    public ResponseEntity<?> deleteProvider(@PathVariable String id) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(id)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            Boolean result = mailProviderService.delete(id);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("deleteProvider exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param mailMessage
     * @return
     */
    @PostMapping("/crud/mail/sendMessage")
    public Mono<ResponseEntity<?>> sendMessage(@RequestPart(value = "files", required = false) Flux<FilePart> files,
                                               @RequestPart("mailMessage") String mailMessage) {
        return Mono.deferContextual(context -> {
            LoginUser loginUser = context.getOrDefault("loginUser", null);
            if (loginUser == null) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }
            log.info("mailMessage={}", mailMessage);
            MailSendMessageRequest mailSendMessageRequest;
            try {
                mailSendMessageRequest = JSON.parseObject(mailMessage, MailSendMessageRequest.class);
            } catch (Exception ex) {
                log.info("--- JSON转换失败：{}", ex.getMessage());
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
            }

            //        String email = mailSendMessageRequest.getEmail();
            InternetAddress[] recipients = mailSendMessageRequest.getRecipients();
            InternetAddress[] ccRecipients = mailSendMessageRequest.getCcRecipients();
            InternetAddress[] replyTo = mailSendMessageRequest.getReplyTo();
            InternetAddress[] from = mailSendMessageRequest.getFrom();

            List<File> myFiles = mailSendMessageRequest.getMyFiles();
            List<File> originFiles = mailSendMessageRequest.getOriginFiles();

            if (recipients.length == 0 || from.length == 0) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }

            String senderEmail = from[0].getAddress();

            log.info("----------------------email={}", senderEmail);

            MailProvider mailProvider = mailProviderService.queryBy(senderEmail, loginUser.getUid());

            // 收发邮件的基本信息
            String host = mailProvider.getSmtpHost(); // SMTP服务器地址
            String port = mailProvider.getSmtpPort();             // SMTP服务器端口
            String password = mailProvider.getPassword();         // 邮箱密码或授权码

            // 配置邮件会话属性
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.ssl.enable", "true"); // 启用 SSL


            log.info("---------------------------创建会话");
            // 创建会话
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, password);
                }
            });
//            log.info("---------------------------开始发送");
            // 获取文件个数
            Mono<Long> fileCountMono = files.count();
            // 判断文件数量是否大于0
            return fileCountMono.publishOn(Schedulers.boundedElastic()).publishOn(Schedulers.boundedElastic()).flatMap(fileCount -> {

                ActionMailResult actionMailResult = null;
                try {
                    MimeMessage message = new MimeMessage(session);
                    message.setFrom(from[0]); // 发件人
                    message.addRecipients(Message.RecipientType.TO, recipients); // 收件人
                    if (ccRecipients != null && ccRecipients.length > 0) {
                        log.info("--- 有抄送人");
                        message.addRecipients(Message.RecipientType.CC, ccRecipients); // 抄送人
                    } else {
                        log.info("--- 没有抄送人");
                    }
                    message.setSubject(mailSendMessageRequest.getSubject()); // 邮件主题

                    // 创建邮件正文
                    MimeBodyPart textPart = new MimeBodyPart();
                    // 注意：这里要加text/html，否则邮箱客户端不能正确解析出内容，显示的是文本内容
                    textPart.setContent(mailSendMessageRequest.getContent(), "text/html; charset=utf-8");
                    // 创建多部分邮件内容
                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(textPart);

                    if (!myFiles.isEmpty()) {// 我的文件中的附件
                        log.info("================= 有我的文件中的附件");
                        for (File mailAttachment : myFiles) {
                            if (StringUtils.hasLength(mailAttachment.getUrl())) {
                                log.info("--- 开始添加附件");
                                log.info("---origin url:{} ---", mailAttachment.getUrl());
                                // 从 URL 获取附件
                                MimeBodyPart attachmentPart = new MimeBodyPart();
                                URL url = new URL(mailAttachment.getUrl());  // 附件 URL
                                DataSource source = new URLDataSource(url);
                                attachmentPart.setDataHandler(new DataHandler(source));
                                attachmentPart.setFileName(mailAttachment.getName());
                                multipart.addBodyPart(attachmentPart);  // 添加附件
                            }
                        }
                    }
                    // 查询原邮件中的附件列表，注意：根据前端传递过来的文件列表做比对，如果前端附件列表中没有对应的原附件名称，说明客户已删除，所以我们也不用添加到邮件附件中，只需添加留下来的附件
                    if (mailSendMessageRequest.getMid() != null
                            && StringUtils.hasLength(mailSendMessageRequest.getFolderName())
                            && !originFiles.isEmpty()) {// 有mid有folderName说明是转发
//                        log.info("---");
                        // 查询原始邮件
                        actionMailResult = getMessageByMailSession(mailProvider, mailSendMessageRequest.getFolderName(), mailSendMessageRequest.getMid());
                        Message originMessage = actionMailResult.getMessage();
                        // 处理附件
                        if (originMessage != null && originMessage.isMimeType("multipart/*")) {
                            log.info("================= 有原附件");
                            MimeMultipart originalMultipart = (MimeMultipart) originMessage.getContent();
                            for (int i = 0; i < originalMultipart.getCount(); i++) {
                                MimeBodyPart originAttachmentPart = (MimeBodyPart) originalMultipart.getBodyPart(i);
                                if (Part.ATTACHMENT.equalsIgnoreCase(originAttachmentPart.getDisposition())) {
                                    boolean flag = false;
                                    for (File file : originFiles) {
                                        if (originAttachmentPart.getFileName().equals(file.getName())) {
                                            flag = true;
                                        }
                                    }
                                    // 原附件名称中找到说明要添加此附件
                                    if (flag) {
                                        log.info("---找到了，正在添加到转发邮件附件中");
                                        // 处理附件
                                        MimeBodyPart attachmentCopy = new MimeBodyPart();
                                        attachmentCopy.setDataHandler(originAttachmentPart.getDataHandler());
                                        attachmentCopy.setFileName(originAttachmentPart.getFileName());
                                        multipart.addBodyPart(attachmentCopy);
                                    }
                                }
                            }
                        }
                    }
                    // 使用 Mono 来处理文件流
                    ActionMailResult finalActionMailResult = actionMailResult;
                    // 有文件的情况
                    if (fileCount > 0) { // 有本地上传的文件
                        log.info("================= 有本地文件");


                        return files.collectList().flatMap(fileParts -> {
                            try {
                                List<Mono<Void>> attachmentMemos = new ArrayList<>();

                                // 处理附件
                                for (FilePart filePart : fileParts) {
                                    // 获取文件名
                                    String filename = filePart.filename();
                                    log.info("--- 文件名：{}", filename);

                                    Mono<byte[]> fileBytesMono = filePart.content()
                                            .collectList()
                                            .flatMap(dataBuffers -> {
                                                // 使用 Flux.fromIterable 将 List 转换为 Flux
                                                Flux<DataBuffer> bufferFlux = Flux.fromIterable(dataBuffers);

                                                // 使用 DataBufferUtils.join 来合并所有 DataBuffer
                                                return DataBufferUtils.join(bufferFlux)
                                                        .map(buffer -> {
                                                            byte[] content = new byte[buffer.readableByteCount()];
                                                            buffer.read(content);
                                                            return content;
                                                        });
                                            });

                                    // 构建附件并添加到邮件
                                    MimeBodyPart attachmentPart = new MimeBodyPart();
                                    Mono<Void> attachmentMono = fileBytesMono.flatMap(fileContent -> {
                                        try {
                                            log.info("---正在添加本地文件到邮件附件中---{}", filename);
                                            DataSource source = new ByteArrayDataSource(fileContent, "application/octet-stream");
                                            attachmentPart.setDataHandler(new DataHandler(source));
                                            attachmentPart.setFileName(filename);
                                            // 将文件流放到邮件邮件中
                                            multipart.addBodyPart(attachmentPart);
                                            return Mono.empty();
                                        } catch (MessagingException ex) {
                                            log.info("--- 设置附件流失败：{}", ex.getMessage());
                                            return Mono.error(new Exception("附件处理失败"));
                                        }
                                    });

                                    // 将附件处理任务加入列表
                                    attachmentMemos.add(attachmentMono);
                                }

                                // 等待所有附件都处理完再发送邮件
                                return Mono.when(attachmentMemos)
                                        .then(Mono.fromCallable(() -> {
                                            // 设置邮件的内容和附件
                                            message.setContent(multipart);
                                            // 发送邮件
                                            Transport.send(message);
                                            log.info("邮件发送成功！");
                                            closeMailImap(finalActionMailResult);
                                            return ResponseEntity.ok(new ResultSuccess<>(true));
                                        }))
                                        .onErrorResume(ex -> {
                                            closeMailImap(finalActionMailResult);

                                            log.error("发送邮件失败1: {}", ex.getMessage());
                                            // 返回与原始类型一致的 ResultSuccess 类型的响应
                                            return Mono.just(ResponseEntity.ok(new ResultSuccess<>(false)));
                                        });
                            } catch (Exception e) {
                                closeMailImap(finalActionMailResult);

                                log.error("发送邮件失败2: {}", e.getMessage());
                                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
                            }
                        });
                    } else { // 没有本地上传文件
                        log.info("--- 附件中没有本地上传文件");
                        // 设置邮件内容
                        message.setContent(multipart);
                        // 发送邮件
                        Transport.send(message);
                        log.info("邮件发送成功！");
                        closeMailImap(finalActionMailResult);
                        return Mono.just(ResponseEntity.ok(new ResultSuccess<>(true)));
                    }
                } catch (Exception ex) {
                    log.info("发送邮件失败：{}", ex.getMessage());
                    return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
                }
            });
        });
    }

    /**
     * 获取邮件分页列表
     *
     * @return
     */
    @PostMapping("/crud/mail/getMessages")
    public ResponseEntity<?> getMessages(@RequestBody MailMessageRequest mailMessageRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        Integer pageSize = mailMessageRequest.getPageSize();
        Integer pageNum = mailMessageRequest.getPageNum();

        log.info("mailMessageRequest============{}", JSONArray.toJSONString(mailMessageRequest));
        String email = mailMessageRequest.getEmail();
        String folderName = mailMessageRequest.getFolderName();
        Boolean isUnread = mailMessageRequest.getIsUnread();

        if (!StringUtils.hasLength(folderName) || !StringUtils.hasLength(email)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        if (pageNum == null || pageSize == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            QueryResults result = null;
            if (isUnread == null) {
                result = mailMessageService.queryBy(loginUser.getUid(), email, folderName, pageNum, pageSize);
            } else {
                result = mailMessageService.queryBy(loginUser.getUid(), email, folderName, isUnread, pageNum, pageSize);
            }
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getProvider exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 设置邮件为已读
     *
     * @param mailMessageRequest
     * @return
     */
    @PostMapping("/crud/mail/setMessage")
    public ResponseEntity<?> setMessage(@RequestBody MailMessageRequest mailMessageRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String messageId = mailMessageRequest.getMessageId();
        if (!StringUtils.hasLength(messageId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            Boolean result = mailMessageService.setMessage(loginUser.getUid(), messageId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("setMessage exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除邮件
     *
     * @param emailId
     * @return
     */
    @DeleteMapping("/crud/mail/delete/{emailId}")
    public ResponseEntity<?> delete(@PathVariable String emailId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(emailId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            Boolean result = mailMessageService.removeEmail(emailId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("setMessage exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据mid和附件名上传附件到OSS返回url，前端根据这个url再下载文件
     *
     * @param mailMessageRequest
     * @return
     */
    @PostMapping("/crud/mail/attachment/upload")
    public ResponseEntity<?> uploadAttachment(@RequestBody MailMessageRequest mailMessageRequest) {

        String email = mailMessageRequest.getEmail();
        String folderName = mailMessageRequest.getFolderName();
        Long mid = mailMessageRequest.getMid();
        String fileName = mailMessageRequest.getName();
        String fileId = mailMessageRequest.getFileId();
        String fileType = mailMessageRequest.getFileType();

        log.info("email={},folderName={},mid={},fileName={}", email, folderName, mid, fileName);

        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(folderName)
                || !StringUtils.hasLength(email)
                || mid == null
                || !StringUtils.hasLength(fileName)
                || !StringUtils.hasLength(fileId)
                || !StringUtils.hasLength(fileType)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        ActionMailResult actionMailResult = null;
        try {
            MailProvider mailProvider = mailProviderService.queryBy(email);
            actionMailResult = getMessageByMailSession(mailProvider, folderName, mid);
            if (actionMailResult.getMessage() == null) {
                log.info("邮件未找到");
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.QUERY_EMPTY));
            }
            String fileUrl = uploadAttachmentToOSS(loginUser, actionMailResult.getMessage(), mailMessageRequest);
            return ResponseEntity.ok(new ResultSuccess<>(fileUrl));
        } catch (Exception e) {
            log.info("exception ={}", e.getMessage());
            // 下载失败
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.SERVER_Degrade_Exception));
        } finally {
            // 确保无论如何都关闭 store 连接
            assert actionMailResult != null;
            closeMailImap(actionMailResult);
        }
    }

    private void closeMailImap(ActionMailResult actionMailResult) {
        //        if (actionMailResult.getFolder() != null) {
//            try {
//                actionMailResult.getFolder().close();
//            } catch (MessagingException e) {
//                log.error("关闭 IMAPFolder 连接失败", e);
//            }
//        }
        if (actionMailResult != null && actionMailResult.getStore() != null && actionMailResult.getStore().isConnected()) {
            try {
                actionMailResult.getStore().close();
            } catch (MessagingException e) {
                log.error("关闭 IMAPStore 连接失败", e);
            }
        }
    }

    /**
     * 查询指定邮件
     *
     * @param mailProvider
     * @param folderName
     * @param mid
     * @return
     * @throws MessagingException
     */
    private ActionMailResult getMessageByMailSession(MailProvider mailProvider, String folderName, Long mid) throws MessagingException {
        log.info("folderName={},mid={}", folderName, mid);
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

        properties.put("mail.store.protocol", mailProvider.getProtocol());
        properties.put("mail.imap.fetchsize", "1048576");// 设置为1MB（单位为字节）
        properties.put("mail.imap.connectiontimeout", "10000"); // 连接超时时间 10秒
        properties.put("mail.imap.timeout", "20000");          // 读取超时时间 20秒

        log.info("======getDefaultFolder========{}", properties);
//        log.info("======mailProvider========{}", JSONArray.toJSONString(mailProvider));

        Session session = Session.getDefaultInstance(properties);
        IMAPStore store = (IMAPStore) session.getStore(mailProvider.getProtocol());
        log.info("开始连接邮箱");
        store.connect(mailProvider.getImapHost(), mailProvider.getEmail(), mailProvider.getPassword()); // 使用授权码而不是密码

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

        log.info("开始获取文件夹");
        // 获取邮件文件夹
        IMAPFolder folder = (IMAPFolder) store.getFolder(folderName);

        if (folder == null) {
            log.info("没有查询到指定邮件文件夹");
            throw new RuntimeException("没有查询到指定邮件文件夹");
        }
        folder.open(Folder.READ_ONLY);
        Message message = folder.getMessageByUID(mid);
        return new ActionMailResult(store, folder, message);
    }

    private String getFilePath(String uid, String fileName, String timeStamp) throws UnsupportedEncodingException {
        // 对文件名进行编码
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        // 生成UUID
        String uuid = UUID.randomUUID().toString();
        String imgPath = "space/file/" + uid + "/" + timeStamp + "/" + uuid.replace("-", "_") + "/" + encodedFileName;
        return imgPath;
    }

    /**
     * 上传附件到OSS
     *
     * @param loginUser
     * @param message
     * @return
     */
    public String uploadAttachmentToOSS(LoginUser loginUser, Message message, MailMessageRequest mailMessageRequest) {
//        List<MailAttachment> attachments = new ArrayList<>();
        try {
            String email = mailMessageRequest.getEmail();
            String folderName = mailMessageRequest.getFolderName();
            Long mid = mailMessageRequest.getMid();
            String fileName = mailMessageRequest.getName();
            String fileId = mailMessageRequest.getFileId();
            String fileType = mailMessageRequest.getFileType();
            log.info("---fileName:{}---", fileName);

//            log.info("fetchAttachments message ={}", JSONArray.toJSONString(message));
            log.info("根据mid获取邮件成功");
            if (message.isMimeType("multipart/*") && message.getContent() instanceof MimeMultipart mimeMultipart) {

                log.info("已确认邮件包含附件");
                for (int i = 0; i < mimeMultipart.getCount(); i++) {
                    BodyPart part = mimeMultipart.getBodyPart(i);

                    String partFileName = part.getFileName();
                    log.info("---partFileName:{}---", partFileName);
                    if (partFileName != null) {
                        partFileName = MimeUtility.decodeText(partFileName); // 解码文件名
                    }else{
                        // 没有值判断个毛
                        continue;
                    }
                    if (StringUtils.hasLength(fileName) && StringUtils.hasLength(partFileName) && !partFileName.equals(fileName)) {
                        // 找到指定文件名的附件进行下载，如果不是则继续下个循环
                        log.info("我要的找的是{}，而不是{}，所以继续寻找", fileName, partFileName);
                        continue;
                    }
                    log.info("获取到part partFileName={}", partFileName);
                    // 如果是附件
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) ||
                            part.isMimeType("application/octet-stream") ||
                            Part.INLINE.equalsIgnoreCase(part.getDisposition())) {
                        log.info("开始获取附件流");

                        InputStream inputStream = part.getInputStream();

                        if (inputStream == null) {
                            log.info("文件流为空");
                            throw new IllegalStateException("InputStream is null");
                        }
                        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

                        String encodeName = URLEncoder.encode(partFileName, "UTF-8");

                        String filePath = getFilePath(loginUser.getUid(), encodeName, timeStamp);

                        String fileUrl = storageService.uploadFile(inputStream, filePath);
                        log.info("filePath={}", filePath);
                        log.info("fileUrl={}", fileUrl);
                        log.info("fileSize={}", part.getSize());
                        if (StringUtils.hasLength(fileUrl)) {
                            // 查询同名文件并删除
                            fileService.findFileAndDelete(loginUser.getUid(), fileName);
                            Date date = new Date();
                            File fileModel = new File();
                            fileModel.setUid(loginUser.getUid());
//                            if (!pid.equals("-1")) {
//                                fileModel.setPid(pid);
//                            }
//                            if (parentFile == null) {
                            fileModel.setFullPath("/" + partFileName);
//                            } else {
//                                fileModel.setFullPath(parentFile.getFullPath() + partFileName);
//                            }

                            fileModel.setName(partFileName);
                            fileModel.setType(fileType);
                            // 注意：不能使用inputStream.available()，InputStream.available() 方法返回的是可以不阻塞地读取的字节数，而不是流中数据的总大小
                            fileModel.setSize((long) part.getSize());
                            fileModel.setUrl(fileUrl);
                            fileModel.setPath(filePath);
                            fileModel.setIsPrivate(false);
                            fileModel.setIsDelete(false);
                            fileModel.setCreatedDt(date);
                            fileModel.setUpdatedDt(date);
                            fileService.add(fileModel);
                            // 记得使用完后关闭 InputStream
                            inputStream.close();
                            log.info("保存成功，返回ossurl");
                            //保存和上传之后，再把mailMessage中附件的数据也要改一下
                            mailMessageService.setAttachment(loginUser.getUid(), email, folderName, mid, fileId, fileUrl, (long) part.getSize());
                            return fileUrl;
                        }
                    } else {
                        log.info("Part {} is not an attachment", i);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.info("获取附件失败：" + e.getMessage());
            return null;
        }
    }

    public List<HashMap> getAddressList(Address[] addresses) {
        List<HashMap> list = new ArrayList<>();
        if (addresses != null) {
            for (Address address : addresses) {
                if (address instanceof InternetAddress) {
                    String emailAddress = ((InternetAddress) address).getAddress();
                    String personal = ((InternetAddress) address).getPersonal();
//                    log.info("Email Address: {}", emailAddress);
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

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        // 如果消息是 "text/plain" 类型，直接提取文本内容
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        }
        // 如果消息是 multipart 类型
        else if (message.isMimeType("multipart/*") && message.getContent() instanceof MimeMultipart mimeMultipart) {
            return getTextFromMimeMultipart(mimeMultipart);
        }
        // 如果消息是 "text/html" 类型，提取 HTML 内容
        else if (message.isMimeType("text/html")) {
            return message.getContent().toString();
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder plainText = new StringBuilder();
        StringBuilder htmlText = new StringBuilder();
        int count = mimeMultipart.getCount();
        List<String> attachmentLinks = new ArrayList<>(); // 用于存储附件链接

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            // 提取 "text/plain" 内容
            if (bodyPart.isMimeType("text/plain")) {
                plainText.append(bodyPart.getContent());
            }
            // 提取 "text/html" 内容
            else if (bodyPart.isMimeType("text/html")) {
                htmlText.append(bodyPart.getContent());
            }
            // 处理附件
            else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                    Part.INLINE.equalsIgnoreCase(bodyPart.getDisposition())) {
                // 这里可以实现附件的保存逻辑，比如将附件保存到文件系统或其他存储
                // 使用 MimeUtility.decodeText 进行文件名解码
                String fileName = MimeUtility.decodeText(bodyPart.getFileName());
                log.info("----fileName-----{}", fileName);
                InputStream is = bodyPart.getInputStream();
                // 可以保存文件，也可以将其存储在其他对象中以便以后处理
                // 示例：保存文件到系统
//                Files.copy(is, Paths.get("attachments", fileName), StandardCopyOption.REPLACE_EXISTING);
                attachmentLinks.add("<a href=\"#\">" + fileName + "</a>");

            }
        }
        // 优先返回 HTML 内容，如果没有则返回纯文本内容
        String content = !htmlText.isEmpty() ? htmlText.toString() : plainText.toString();

        // 在内容底部添加附件链接的 HTML 结构
        if (!attachmentLinks.isEmpty()) {
            content += "<br><br><strong>附件:</strong><br>" + String.join("<br>", attachmentLinks);
        }
        return content;
    }
}
