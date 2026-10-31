package com.seaurl.platform.users.service.aliyun;

import java.io.IOException;
import java.io.InputStream;


public interface OssService {
    String upload(String base64Img, String folder) throws IOException;

    Boolean copyTo(String sourceFilePath, String targetFilePath);
}
