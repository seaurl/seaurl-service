package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.FileTransferConfig;

import java.util.HashMap;
import java.util.List;

public interface FileTransferConfigService {

    List<HashMap> queryBy(String uid);

    FileTransferConfig query(String uid);

    FileTransferConfig add(FileTransferConfig fileTransferConfig);

    boolean update(FileTransferConfig fileTransferConfig);
}
