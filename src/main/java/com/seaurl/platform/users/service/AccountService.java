package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.request.AccountNotice;
import com.seaurl.platform.users.request.AccountRequest;
import com.seaurl.platform.users.request.UserRequest;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

public interface AccountService {
    boolean isExistUsername(String username);

    boolean isExistEmail(String email);


    boolean updateUserAvatar(String userName, String avatar);

    List<User> findAll();

    User getUserByName(String username);

    Mono<User> getUserByNameMono(String username);

    User getUserById(String uid);

    Mono<User> getUserByIdMono(String uid);

    User getUserByEmail(String email);

    void updateUserInfo(UserRequest userRequest) throws Exception;

    void updateUserIpAddr(String uid, String region);

    String saveAvatar(String base64Img, String folder) throws IOException;

    boolean updateUserName(String userName, String newUserName);

    boolean updateUserNotice(AccountNotice accountNotice);

    boolean deleteAccount(String uid);

    boolean updateAppearance(String uid, String color, String background);
}
