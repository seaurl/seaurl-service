package com.seaurl.platform.users.service;


import com.seaurl.platform.users.request.RegisterRequest;

public interface RegisterService {
    String register(RegisterRequest registerRequest);

    boolean hasUsername(String username);

    boolean hasEmail(String email);

}
