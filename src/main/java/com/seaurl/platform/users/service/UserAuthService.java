package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.model.UserAuth;

public interface UserAuthService {

    boolean updateUserPassword(String username,String email,String pwd);

    boolean updateUserPassword(String userName, String newPwd);

    boolean updateUserName(String userName, String newUserName);

    boolean updateUserEmail(String userName, String newEmail);

    boolean updateUserVerified(String username);

    boolean isVerified(String username);

    UserAuth getUserByName(String username);

    User getUserByEmail(String email);
}
