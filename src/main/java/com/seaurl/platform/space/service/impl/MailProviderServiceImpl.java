package com.seaurl.platform.space.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.ImageAi;
import com.seaurl.platform.space.model.MailProvider;
import com.seaurl.platform.space.service.MailProviderService;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
public class MailProviderServiceImpl implements MailProviderService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            }
        }
        return result.toString();
    }

    /**
     * 添加新邮箱，并拉取邮件
     *
     * @param mailProvider
     * @return
     */
    @Override
    public MailProvider add(MailProvider mailProvider) {
        MailProvider result = mongoTemplate.insert(mailProvider);
        return result;
    }

    @Override
    public Boolean update(MailProvider mailProvider) {
        Date date = new Date();
        Query query = new Query(Criteria.where("email").is(mailProvider.getEmail()));
        Update update = new Update();
        update.set("name", mailProvider.getName());
        update.set("nickName", mailProvider.getNickName());
        update.set("updatedDt", date);
        UpdateResult result = mongoTemplate.updateFirst(query, update, MailProvider.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Boolean updateStatus(String email, String uid, String status) {
        Date date = new Date();
        Query query = new Query(Criteria.where("email").is(email).and("uid").is(uid));
        Update update = new Update();
        update.set("status", status);
        update.set("updatedDt", date);
        if ("1".equals(status)) {
            update.set("lastSync", date);
        }
        UpdateResult result = mongoTemplate.updateFirst(query, update, MailProvider.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Boolean delete(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        DeleteResult result = mongoTemplate.remove(query, MailProvider.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public boolean create(MailProvider mailProvider) {
        log.info("--- mailProvider --- ={}", JSONArray.toJSONString(mailProvider));

        Properties properties = new Properties();
        if (mailProvider.getProtocol().equals("imap")) {
            properties.put("mail.imap.host", mailProvider.getImapHost());
            properties.put("mail.imap.port", mailProvider.getImapPort());
            properties.put("mail.imap.ssl.enable", mailProvider.getImapSslEnable());  // 启用SSL
        }
        if (mailProvider.getProtocol().equals("pop")) {
            properties.put("mail.pop.host", mailProvider.getPopHost());
            properties.put("mail.pop.port", mailProvider.getPopPort());
            properties.put("mail.imap.ssl.enable", mailProvider.getPopSslEnable());  // 启用SSL

        }
        properties.put("mail.store.protocol", mailProvider.getProtocol());

//        properties.put("mail.imaps.ssl.enable", "true");
        try {
            Session session = Session.getDefaultInstance(properties);
            Store store = session.getStore(mailProvider.getProtocol());

            store.connect(mailProvider.getImapHost(), mailProvider.getEmail(), mailProvider.getPassword()); // 使用授权码而不是密码
            Folder[] folders = store.getDefaultFolder().list();
            for (Folder folder : folders) {
//                System.out.println("--- Folder: " + folder.getFullName());
                // 获取收到邮件的文件夹
                Folder folder2 = store.getFolder(folder.getFullName());
                folder2.open(Folder.READ_ONLY);
//            // 获取发送邮件的文件夹
//                Folder sentFolder = store.getFolder("Sent"); // 有的邮箱可能是 "Sent Items" 或 "Sent Messages"
//                sentFolder.open(Folder.READ_ONLY);

                int batchSize = 30;
                int totalMessages = folder2.getMessageCount();
                log.info("totalMessages ={}", totalMessages);
                for (int i = 1; i <= totalMessages; i += batchSize) {
                    int end = Math.min(i + batchSize - 1, totalMessages);
                    Message[] messages = folder2.getMessages(i, end);

                    for (Message message : messages) {
                        System.out.println("Subject: " + message.getSubject());
                        System.out.println("From: " + message.getFrom()[0]);
                        System.out.println("Content: " + getTextFromMessage(message));
                        Date receivedDate = message.getReceivedDate();
                        System.out.println("receivedDate: " + receivedDate);
                        // 判断是否是红旗邮件
                        if (message.isSet(Flags.Flag.FLAGGED)) {
                            System.out.println("This email is flagged (important).");
                        }
                        // 判断是否是草稿邮件
                        if (message.isSet(Flags.Flag.DRAFT)) {

                        }
                        // 判断是否是删除邮件
                        if (message.isSet(Flags.Flag.DELETED)) {

                        }
                        // 判断是否是回复的邮件
                        if (message.isSet(Flags.Flag.ANSWERED)) {

                        }
                        // 判断是否是最近收到的邮件
                        if (message.isSet(Flags.Flag.RECENT)) {

                        }
                        // 判断是否是已读的邮件
                        if (message.isSet(Flags.Flag.SEEN)) {

                        }
                        // 自定义标识的邮件
                        if (message.isSet(Flags.Flag.USER)) {

                        }
//
//                        message.getAllRecipients();
//
//                    System.out.println("Subject: " + message.getSubject());
//                    System.out.println("From: " + message.getFrom()[0]);
//                    System.out.println("Content: " + getTextFromMessage(message));
                    }
                }
                folder2.close(false);
            }
            store.close();
            return true;
        } catch (Exception ex) {
            log.info("exception ={}", ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean isExistEmail(String uid,String email) {
        Query query = new Query(Criteria.where("uid").is(uid).and("email").is(email));
        MailProvider user = mongoTemplate.findOne(query, MailProvider.class);
        return user != null;
    }

    @Override
    public boolean isExistName(String uid, String name) {
        Query query = new Query(Criteria.where("uid").is(uid).and("name").is(name));
        MailProvider result = mongoTemplate.findOne(query, MailProvider.class);
        return result != null;
    }

    @Override
    public List<MailProvider> queryAll(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, MailProvider.class);
    }

    @Override
    public MailProvider queryBy(String email, String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("email").is(email));
        return mongoTemplate.findOne(query, MailProvider.class);
    }

    @Override
    public MailProvider queryBy(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.findOne(query, MailProvider.class);
    }
}
