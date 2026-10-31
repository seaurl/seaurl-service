package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.UserCopyUrl;

import java.util.HashMap;
import java.util.List;

public interface UserCopyUrlService {

    boolean addCopyUrl(UserCopyUrl userCopyUrl);

    List<HashMap> getUrlCopyList(String uid);
}
