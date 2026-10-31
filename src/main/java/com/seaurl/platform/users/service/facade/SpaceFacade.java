package com.seaurl.platform.users.service.facade;

import com.seaurl.platform.common.model.File;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.Url;
import reactor.core.publisher.Mono;

public interface SpaceFacade {
    Mono<Url> getUrlById(String urlId);
    Mono<File> getSharedFriends(String fileId, LoginUser loginUser);
}
