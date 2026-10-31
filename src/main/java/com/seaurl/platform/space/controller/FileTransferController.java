package com.seaurl.platform.space.controller;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;
import com.seaurl.platform.common.model.File;
import com.seaurl.platform.space.model.FileTransfer;
import com.seaurl.platform.space.model.FileTransferConfig;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.request.FileRequest;
import com.seaurl.platform.space.request.FileTransferConfigRequest;
import com.seaurl.platform.space.request.FileTransferRequest;
import com.seaurl.platform.space.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/space")

public class FileTransferController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    FileTransferService fileTransferService;

    @Autowired
    FileTransferConfigService fileTransferConfigService;

    @Autowired
    AliyunConfig aliyunOssConfig;

    @Autowired
    FileService fileService;

    @Autowired
    StorageService storageService;


    // 重新命名文件名
    private String reFileName(String fileName, String timeStamp) {
        // 获取文件名和扩展名
        String baseName = fileName.substring(0, fileName.lastIndexOf(".")); // aaa
        String extension = fileName.substring(fileName.lastIndexOf("."));   // .docx

        // 修改文件名为 "aaa_副本"
        return baseName + "_" + timeStamp + extension;
    }


    /**
     * 查询文件助手保存的路径
     *
     * @return
     */
    @GetMapping("/crud/fileTransferConfig/query")
    public ResponseEntity<?> queryFileTransferConfig() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        try {
            FileTransferConfig result = fileTransferConfigService.query(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.info("queryFileTransferConfig error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 添加文件助手文件保存的路径
     *
     * @param fileTransferConfigRequest
     * @return
     */
    @PostMapping("/crud/fileTransferConfig/addOrUpdate")
    public ResponseEntity<?> addOrUpdateFileTransferConfig(@RequestBody FileTransferConfigRequest fileTransferConfigRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        String pid = fileTransferConfigRequest.getPid();
        if (!StringUtils.hasLength(pid)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            FileTransferConfig queryConfig = fileTransferConfigService.query(loginUser.getUid());
            Date current = new Date();
            FileTransferConfig fileTransferConfig = new FileTransferConfig();
            fileTransferConfig.setUid(loginUser.getUid());
            // 说明不是根目录，如果是-1说明是根目录，不需要保存pid
            if (!pid.equals("-1")) {
                fileTransferConfig.setPid(pid);
            }

            if (queryConfig != null) {
                fileTransferConfig.setUpdatedDt(current);

                boolean result = fileTransferConfigService.update(fileTransferConfig);
                if (result) {
                    return ResponseEntity.ok(new ResultSuccess<>(fileTransferConfig));
                } else {
                    return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
                }
            } else {
                fileTransferConfig.setCreatedDt(current);
                fileTransferConfig.setUpdatedDt(current);

                FileTransferConfig result = fileTransferConfigService.add(fileTransferConfig);
                return ResponseEntity.ok(new ResultSuccess<>(result));
            }
        } catch (Exception ex) {
            log.info("queryFileTransferConfig error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * @param fileTransferRequest
     * @return
     */
    @PostMapping("/crud/fileTransfer/query")
    public ResponseEntity<?> queryFileTransferList(@RequestBody FileTransferRequest fileTransferRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String date = fileTransferRequest.getDate();

        if (!StringUtils.hasLength(date)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            List<HashMap> result = fileTransferService.queryBy(loginUser.getUid(), date);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.info("queryFileTransferList error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param fileTransferRequest
     * @return
     */
    @PostMapping("/crud/fileTransfer/queryByMonth")
    public ResponseEntity<?> queryByMonth(@RequestBody FileTransferRequest fileTransferRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String date = fileTransferRequest.getDate();

        if (!StringUtils.hasLength(date)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            List<HashMap> result = fileTransferService.queryByMonth(loginUser.getUid(), date);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.info("queryFileTransferList error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除消息，不删除文件
     *
     * @param id
     * @return
     */
    @DeleteMapping("/crud/fileTransfer/delete/{id}")
    public ResponseEntity<?> deleteFileTransfer(@PathVariable String id) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(id)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = fileTransferService.delete(id);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.info("queryFileTransferList error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param file
     * @param content
     * @param date
     * @param aiToolId
     * @return
     */
    @PostMapping("/crud/fileTransfer/add")
    public Mono<ResponseEntity<?>> addFileTransfer(
            // 有file说明是从本地文件或者剪切板添加
            @RequestPart(value = "file", required = false) Mono<FilePart> file,
            // 有fileId说明是从我的文件中添加的
            @RequestPart(value = "fileId", required = false) String fileId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "date", required = true) String date,
            @RequestPart(value = "aiToolId", required = true) String aiToolId) {

        log.info("content----{}", content);
        log.info("date----{}", date);
        log.info("aiToolId----{}", aiToolId);

        return Mono.defer(() -> {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }
            if (!StringUtils.hasLength(date) || !StringUtils.hasLength(aiToolId)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }
            try {
                Date current = new Date();
                return file.hasElement()
                        .flatMap(hasFile -> {
                            HashMap<String, Object> result = new HashMap<>();
                            // 判断是否有文件
                            if (hasFile) {
                                // 上传文件情况下保存FileTransfer
                                FileTransferConfig fileTransferConfig = fileTransferConfigService.query(loginUser.getUid());
                                File parentFile;
                                if (fileTransferConfig != null && StringUtils.hasLength(fileTransferConfig.getPid())) {
                                    parentFile = fileService.queryFileById(fileTransferConfig.getPid());
                                } else {
                                    parentFile = null;
                                }
//
//                                // 用户文件保存的路径不能为空
//                                if (fileTransferConfig == null) {
//                                    return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//                                }

                                // 处理 file 存在的逻辑
                                return file.flatMap(filePart -> {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    return filePart.content().collectList().flatMap(dataBuffers -> {

                                        try {

                                            //1、构造InputStream
                                            dataBuffers.forEach(buffer -> {
                                                byte[] bytes = new byte[buffer.readableByteCount()];
                                                buffer.read(bytes);
                                                baos.write(bytes, 0, bytes.length);
                                            });
                                            byte[] bytes = baos.toByteArray();
                                            Long fileSize = (long) bytes.length;
                                            String fileName = filePart.filename();
                                            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
// 获取当前时间戳
                                            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                                            // 查询是否存在相同文件(注意：网盘里面的路径对应的文件肯定是唯一的，所以只能从数据库端判断是否存在相同的文件)
//                                            boolean isExist = fileService.isExist(loginUser.getUid(), fileTransferConfig.getCategoryId(), fileName);
//                                            if (isExist) {
//                                                // 如果存在则重新命名文件名
//                                                fileName = reFileName(fileName);
//                                            }

                                            // 不能使用用户名，可以换成id
                                            // 注意：这里加入时间戳是因为fileName可能相同所以加入时间戳可以保持唯一 性
//                                            String imgPath = "space/file/" + loginUser.getUid() + "/" + System.currentTimeMillis() + "/" + fileName;


                                            String prevPath = parentFile == null ? "/" : parentFile.getFullPath();
                                            // 查询是否存在同名文件
                                            boolean isExist = fileService.searchFolderPathIsExistFile(loginUser.getUid(), prevPath, fileName, false);
                                            if (isExist) {
                                                // 如果存在则重新命名文件名
                                                fileName = reFileName(fileName, timeStamp);
                                                log.info("重新命名了，新名字是：{}", fileName);
                                            }
                                            // 生成UUID
                                            String uuid = UUID.randomUUID().toString();
                                            String imgPath = "space/file/" + loginUser.getUid() + "/" + timeStamp + "/" + uuid.replace("-", "_") + "/" + fileName;

                                            //2、上传文件到阿里云
                                            String imgUrl = storageService.uploadFile(inputStream, imgPath);
                                            if (StringUtils.hasLength(imgUrl)) {

                                                File fileModel = new File();
                                                fileModel.setUid(loginUser.getUid());

                                                if (fileTransferConfig !=null && StringUtils.hasLength(fileTransferConfig.getPid())) {
                                                    fileModel.setPid(fileTransferConfig.getPid());
                                                }

                                                if (parentFile == null) {
                                                    fileModel.setFullPath("/" + fileName);
                                                } else {
                                                    fileModel.setFullPath(parentFile.getFullPath() + fileName);
                                                }

                                                fileModel.setName(fileName);
                                                String fileType = getFileType(fileName);
                                                fileModel.setType(fileType);
                                                fileModel.setSize(fileSize);
                                                fileModel.setPath(imgPath);
                                                fileModel.setUrl(imgUrl);


                                                fileModel.setIsPrivate(false);
                                                fileModel.setIsDelete(false);
                                                fileModel.setCreatedDt(current);
                                                fileModel.setUpdatedDt(current);

                                                //3、保存文件到File表中
                                                File addFileResult = fileService.add2(fileModel);

                                                //4、保存相关信息到FileTransfer表中
                                                FileTransfer fileTransfer = setFileTransfer(loginUser.getUid(), content, addFileResult.getId(), aiToolId, date);

                                                result.put("fileTransfer", fileTransfer);
                                                result.put("file", addFileResult);

                                                return Mono.just(ResponseEntity.ok(new ResultSuccess<>(result)));
                                            }
                                            // 上传图片报错
                                            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.UPLOAD_ERROR)));
                                        } catch (Exception ex) {
                                            log.info("uploadFileAndRead ex={}", ex.getMessage());
                                            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
                                        }
                                    });
                                });
                            } else {
                                // 非文件情况下保存FileTransfer
                                FileTransfer fileTransfer = setFileTransfer(loginUser.getUid(), content, fileId, aiToolId, date);
                                result.put("fileTransfer", fileTransfer);
                                return Mono.just(ResponseEntity.ok(new ResultSuccess<>(result)));
                            }
                        });
            } catch (Exception ex) {
                log.error("uploadLargeFile error ={}", ex.getMessage());
                return Mono.just(ResponseEntity.ok(new ResultSuccess<>(false)));
            }
        });

    }


    /**
     * 构建FileTransfer
     *
     * @param uid
     * @param content
     * @param fileId
     * @param aiToolId
     * @param actionDate
     * @return
     */
    public FileTransfer setFileTransfer(String uid, String content, String fileId, String aiToolId, String actionDate) {
        Date current = new Date();
        FileTransfer fileTransfer = new FileTransfer();
        if (StringUtils.hasLength(content)) {
            fileTransfer.setContent(content);
            fileTransfer.setType("text");
        } else {
            if (StringUtils.hasLength(fileId)) {
                fileTransfer.setFileId(fileId);
            }
            fileTransfer.setType("file");
        }
        fileTransfer.setAiToolId(aiToolId);
        fileTransfer.setUid(uid);

        fileTransfer.setCreatedDt(current);
        fileTransfer.setUpdatedDt(current);
        fileTransfer.setIsDelete(false);
        fileTransfer.setIsPrivate(false);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 解析字符串为 LocalDateTime
        LocalDate localDate = LocalDate.parse(actionDate, formatter);

        // 转换为 Date
        Date fileTransferActionDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        fileTransfer.setActionDate(fileTransferActionDate);

        return fileTransferService.add(fileTransfer);
    }

    public String getFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null; // 或者可以抛出一个异常
        }

        String suffix = getSuffix(fileName);
        if (suffix == null || suffix.isEmpty()) {
            return "other"; // 或者可以返回null或抛出异常
        }

        suffix = suffix.toLowerCase();

        // 匹配图片
        if (isImageType(suffix)) return "image";
        // 匹配txt
        if (isTxtType(suffix)) return "txt";
        // 匹配excel
        if (isExcelType(suffix)) return "excel";
        // 匹配word
        if (isWordType(suffix)) return "word";
        // 匹配pdf
        if (isPdfType(suffix)) return "pdf";
        // 匹配ppt
        if (isPptType(suffix)) return "ppt";
        // 匹配zip
        if (isZipType(suffix)) return "zip";
        // 匹配视频
        if (isVideoType(suffix)) return "video";
        // 匹配音频
        if (isAudioType(suffix)) return "audio";

        // 其他文件类型
        return "other";
    }

    private String getSuffix(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return null; // 没有找到点号，即没有后缀
        }
        return fileName.substring(lastIndexOfDot + 1);
    }

    private boolean isImageType(String suffix) {
        return "png".equals(suffix) || "jpg".equals(suffix) || "jpeg".equals(suffix) ||
                "bmp".equals(suffix) || "gif".equals(suffix);
    }

    // 类似地，为其他类型添加方法
    private boolean isTxtType(String suffix) {
        return "txt".equals(suffix);
    }

    private boolean isExcelType(String suffix) {
        return "xls".equals(suffix) || "xlsx".equals(suffix);
    }

    private boolean isWordType(String suffix) {
        return "doc".equals(suffix) || "docx".equals(suffix);
    }

    private boolean isPdfType(String suffix) {
        return "pdf".equals(suffix);
    }

    private boolean isPptType(String suffix) {
        return "ppt".equals(suffix) || "pptx".equals(suffix);
    }

    private boolean isZipType(String suffix) {
        return "rar".equals(suffix) || "zip".equals(suffix) || "7z".equals(suffix) || "dmg".equals(suffix);
    }

    private boolean isVideoType(String suffix) {
        return "mp4".equals(suffix) || "m2v".equals(suffix) || "mkv".equals(suffix) ||
                "rmvb".equals(suffix) || "wmv".equals(suffix) || "avi".equals(suffix) ||
                "flv".equals(suffix) || "mov".equals(suffix) || "m4v".equals(suffix);
    }

    private boolean isAudioType(String suffix) {
        return "mp3".equals(suffix) || "wav".equals(suffix); // 注意：原代码中的'wmv'是视频格式，这里已移除
    }
}
