package com.seaurl.platform.users.service.facade;

import com.seaurl.platform.common.model.File;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.space.service.FileService;
import com.seaurl.platform.space.service.UrlService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class LocalSpaceFacade implements SpaceFacade {
    private final MongoTemplate mongoTemplate;

    public LocalSpaceFacade(MongoTemplate mongoTemplate, UrlService urlService, FileService fileService) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Url> getUrlById(String urlId) {
        return Mono.fromSupplier(() -> {
            Query query = new Query(Criteria.where("id").is(urlId));
            return mongoTemplate.findOne(query, Url.class);
        });
    }

    @Override
    public Mono<File> getSharedFriends(String fileId, LoginUser loginUser) {
        return Mono.fromSupplier(() -> {
            Query query = new Query(Criteria.where("id").is(fileId));
            return mongoTemplate.findOne(query, File.class);
        });
    }
}
