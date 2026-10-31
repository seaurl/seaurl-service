package com.seaurl.platform.space.request;

import lombok.Data;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

@Data
public class FileTransferRequest {

    private Mono<FilePart> file;

//    @Nullable
//    private String fileId;

    // 使用的工具id，没有使用usedId是因为，这个更直观，usedId给前端用户看的
    private String aiToolId;

//    @Nullable
//    // type=text的文本内容
//    private String content;

    private String date;
}
