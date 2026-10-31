package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.Domain;
import com.seaurl.platform.common.model.UrlCategory;
import com.seaurl.platform.space.service.DomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class DomainServiceImpl implements DomainService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean addDomain(Domain domain) {
        mongoTemplate.insert(domain);
        return true;
    }

    @Override
    public Domain queryDomainByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return mongoTemplate.findOne(query, Domain.class);
    }
}
