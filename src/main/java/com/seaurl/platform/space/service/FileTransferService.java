package com.seaurl.platform.space.service;

import com.seaurl.platform.common.model.File;
import com.seaurl.platform.space.model.FileTransfer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public interface FileTransferService {
    FileTransfer add(FileTransfer fileTransfer);

    boolean delete(String fileTransferId);

    List<HashMap> queryBy(String uid, String date);


    List<HashMap> queryByMonth(String uid, String dateMonth);
}
