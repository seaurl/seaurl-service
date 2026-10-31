package com.seaurl.platform.space.service.facade;

import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.service.UserService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LocalUsersFacade implements UsersFacade {
    private final MongoTemplate mongoTemplate;
    private final UserService userService;

    public LocalUsersFacade(MongoTemplate mongoTemplate, UserService userService) {
        this.mongoTemplate = mongoTemplate;
        this.userService = userService;
    }

    @Override
    public Mono<User> getAccountInfoByUserName(String userName) {
        return Mono.fromSupplier(() -> userService.getUserByName(userName));
    }

    @Override
    public Mono<User> getAccountInfoByUid(String uid) {
        return Mono.fromSupplier(() -> {
            Query query = new Query(Criteria.where("uid").is(uid));
            return mongoTemplate.findOne(query, User.class);
        });
    }

    @Override
    public Mono<User> getAccountInfoByUseId(String useId) {
        return getAccountInfoByUid(useId);
    }
}
