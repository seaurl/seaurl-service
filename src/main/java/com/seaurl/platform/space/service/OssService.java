package com.seaurl.platform.space.service;

import com.aliyuncs.auth.sts.AssumeRoleResponse;

public interface OssService extends StorageService {
    AssumeRoleResponse getAcsForUploadFile(String userName);
}
