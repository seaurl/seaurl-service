package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.MailFolder;
import com.seaurl.platform.space.service.MailFolderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailFolderServiceImpl implements MailFolderService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public void add(MailFolder mailFolder) {
        mongoTemplate.insert(mailFolder);
    }

    @Override
    public List<MailFolder> addForList(List<MailFolder> mailFolderList) {
        return mongoTemplate.insertAll(mailFolderList).stream().toList();
    }

    @Override
    public List<MailFolder> queryFolder(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.find(query, MailFolder.class);
    }
}
