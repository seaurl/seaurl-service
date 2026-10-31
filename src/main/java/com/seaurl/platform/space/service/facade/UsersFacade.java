package com.seaurl.platform.space.service.facade;

import com.seaurl.platform.common.model.User;
import reactor.core.publisher.Mono;

public interface UsersFacade {
    Mono<User> getAccountInfoByUserName(String userName);
    Mono<User> getAccountInfoByUid(String uid);
    Mono<User> getAccountInfoByUseId(String useId);
}
