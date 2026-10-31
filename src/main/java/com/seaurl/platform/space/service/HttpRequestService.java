package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.HttpRequest;

import java.util.List;

public interface HttpRequestService {

    HttpRequest add(HttpRequest httpRequest);

    Boolean update(HttpRequest httpRequest);

    List<HttpRequest> queryAll(String uid);

    Boolean deleteById(String uid, String id);

    HttpRequest queryById(String id);
}
