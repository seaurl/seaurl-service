package com.seaurl.platform.space.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.auth.TokenSessionService;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.common.model.File;
import com.seaurl.platform.space.request.FileRequest;
import com.seaurl.platform.space.request.NoteRequest;
import com.seaurl.platform.space.request.OfficeRequest;
import com.seaurl.platform.space.service.*;
import com.seaurl.platform.space.service.facade.UsersFacade;
import com.seaurl.platform.common.notification.NotificationDowngradeService;
import io.micrometer.observation.annotation.Observed;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/space")

@Observed
public class FileController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    UsersFacade usersFacade;

    @Autowired
    UserService userService;

    @Autowired
    FileCategoryService fileCategoryService;

    @Autowired
    FileService fileService;

    @Autowired
    FileShareService fileShareService;

    @Autowired
    ImageAiService imageAiService;

    @Autowired
    StorageService storageService;

    @Autowired(required = false)
    OssService ossService;

    @Autowired
    AliyunConfig aliyunOssConfig;

    @Autowired
    NotificationDowngradeService notificationDowngradeService;

    @Autowired
    TokenSessionService tokenSessionService;

    @Value("${storage.type:local}")
    private String storageType;

    private static final long MAX_UPLOAD_FILE_SIZE = 200L * 1024 * 1024;

    /**
     * 构造file path
     *
     * @param uid
     * @param fileName
     * @param timeStamp
     * @return
     */
    private String getFilePath(String uid, String fileName, String timeStamp) throws UnsupportedEncodingException {
        // 对文件名进行编码
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        // 生成UUID
        String uuid = UUID.randomUUID().toString();
        String imgPath = "space/file/" + uid + "/" + timeStamp + "/" + uuid.replace("-", "_") + "/" + encodedFileName;
        return imgPath;
    }

    /**
     * @param uid
     * @param path
     * @param isFullPath
     * @param fileName
     * @param isFolder
     * @param timeStamp
     * @return
     */
    private String reFileName(String uid, String path, boolean isFullPath, String fileName, boolean isFolder, String timeStamp) {
        String prevPath = path;
        if (StringUtils.hasLength(path) && isFullPath) {
            prevPath = getPrevPath(path);
        }

        log.info("prevPath = {},fileName={}", prevPath, fileName);
        boolean isExist = fileService.searchFolderPathIsExistFile(uid, prevPath, fileName, isFolder);
        log.info("isExist = {}", isExist);

        if (isExist) {
            // 如果存在则重新命名文件名
            fileName = reFileName(fileName, isFolder, timeStamp);
            log.info("new fileName = {}", fileName);

        }
        return fileName;
    }

    /**
     * 重新命名文件名
     *
     * @param fileName
     * @param isFolder
     * @param timeStamp
     * @return
     */
    private String reFileName(String fileName, boolean isFolder, String timeStamp) {
        if (isFolder) {
            // 修改文件名为 "aaa_副本"
            return fileName + "_" + timeStamp;
        } else {
            // 获取文件名和扩展名
            String baseName = fileName.substring(0, fileName.lastIndexOf(".")); // aaa
            String extension = fileName.substring(fileName.lastIndexOf("."));   // .docx
            // 修改文件名为 "aaa_副本"
            return baseName + "_" + timeStamp + extension;
        }
    }


    /**
     * 根据路径重命名文件名
     *
     * @param filePath
     * @param isFolder
     * @param timeStamp
     * @return
     */
    private String reFilePath(String filePath, boolean isFolder, String timeStamp) {
        if (isFolder) {
            // 修改后的文件名
            return filePath + "_" + timeStamp;
        } else {
            // 找到最后一个路径分隔符的位置
            int lastSlashIndex = filePath.lastIndexOf('/');
            // 找到最后一个点的位置，确保它在文件名中
            int lastDotIndex = filePath.lastIndexOf('.');

            // 获取文件名（不包括扩展名）和扩展名
            String fileName = filePath.substring(lastSlashIndex + 1, lastDotIndex);
            String fileExtension = filePath.substring(lastDotIndex);

            // 修改后的文件名
            String newFileName = fileName + "_" + timeStamp + fileExtension;
            // 返回修改后的完整路径
            return filePath.substring(0, lastSlashIndex + 1) + newFileName;
        }
    }

    /**
     * 修改fullpath
     *
     * @param fullPath
     * @param isFolder
     * @param timeStamp
     * @return
     */
    private String reFileFullPath(String fullPath, boolean isFolder, String timeStamp) {
        if (isFolder) {
            // 去掉路径末尾的斜杠（如果有），然后添加时间戳
            if (fullPath.endsWith("/")) {
                fullPath = fullPath.substring(0, fullPath.length() - 1);
            }

            // 拼接时间戳，并在末尾加上斜杠
            return fullPath + "_" + timeStamp + "/";
        } else {
            // 找到最后一个路径分隔符的位置
            int lastSlashIndex = fullPath.lastIndexOf('/');
            // 找到最后一个点的位置，确保它在文件名中
            int lastDotIndex = fullPath.lastIndexOf('.');

            // 获取文件名（不包括扩展名）和扩展名
            String fileName = fullPath.substring(lastSlashIndex + 1, lastDotIndex);
            String fileExtension = fullPath.substring(lastDotIndex);

            // 修改后的文件名
            String newFileName = fileName + "_" + timeStamp + fileExtension;
            // 返回修改后的完整路径
            return fullPath.substring(0, lastSlashIndex + 1) + newFileName;
        }
    }


    /**
     * 保存office文档
     *
     * @param officeCallback
     * @return
     */
    @CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
    @PostMapping("/info/file/office/save")
    public Mono<ResponseEntity<?>> handleCallback(@RequestBody OfficeCallback officeCallback) {

        log.info("officeCallback ={}", JSONArray.toJSONString(officeCallback));
//        LoginUser loginUser = UserContextHolder.getContext().getUser();
//        if (loginUser == null) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
//        }
        String key = officeCallback.getKey();
        if (!StringUtils.hasLength(key)) {
            log.info("文件id不能为空");
            return Mono.just(ResponseEntity.<Object>ok(Collections.singletonMap("error", 0)));
        }

        String[] users = officeCallback.getUsers();
        if (users != null && users.length > 0) {
            Mono<User> userMono = usersFacade.getAccountInfoByUseId(users[0]);
            return userMono.map(user -> {

                if (user == null) {
                    log.info("没有查询到用户信息");
                    return ResponseEntity.<Object>ok(Collections.singletonMap("error", 0));
                }
                try {

                    File file = fileService.queryFileById(key);

                    if (file == null) {
                        log.info("没有查询到文件");

                        return ResponseEntity.<Object>ok(Collections.singletonMap("error", 0));
                    }

                    //状态监听
                    //参见https://api.onlyoffice.com/editors/callback
                    Integer status = officeCallback.getStatus();
                    log.info("status ={}", status);
                    switch (status) {
                        case 1: {
                            //document is being edited  文档已经被编辑
                            break;
                        }
                        case 2: {
                            //document is ready for saving,文档已准备好保存
                            log.info("document is ready for saving");
                            String downloadUri = officeCallback.getUrl();
                            if (StringUtils.hasLength(downloadUri)) {
                                Integer result = saveOfficeFile(key, downloadUri, file);
                                return ResponseEntity.<Object>ok(Collections.singletonMap("error", 0));
                            }
                            break;
                        }
                        case 3: {
                            //document saving error has occurred,保存出错
                            break;
                        }
                        case 4: {
                            //document is closed with no changes,未保存退出
                            break;
                        }
                        case 6: {
                            //document is being edited, but the current document state is saved,编辑保存
                            String downloadUri = officeCallback.getUrl();
                            if (StringUtils.hasLength(downloadUri)) {
                                Integer result = saveOfficeFile(key, downloadUri, file);
                                return ResponseEntity.<Object>ok(Collections.singletonMap("error", 0));
                            }
                        }
                        case 7: {
                            //error has occurred while force saving the document. 强制保存文档出错
                        }
                        default: {

                        }
                    }
                    // 返回响应
                    return ResponseEntity.<Object>ok(Collections.singletonMap("error", 0));
                } catch (Exception ex) {
                    log.info("操作异常：{}", ex.getMessage());
                    // 返回响应
                    return ResponseEntity.<Object>ok(Collections.singletonMap("error", 0));
                }
            });
        } else {
            log.info("用户id不能为空");
            return Mono.just(ResponseEntity.<Object>ok(Collections.singletonMap("error", 0)));
        }
    }

    private Integer saveOfficeFile(String key, String downloadUri, File file) throws Exception {
        boolean result = fileService.updateDt(key);
        if (result) {

            URL url = new URL(downloadUri);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            InputStream stream = connection.getInputStream();

            String fullurl = storageService.uploadFile(stream, file.getPath());
            log.info("保存文件成功：{}", fullurl);
            return 0;
        } else {
            log.info("更新文件失败");

            return 1;
        }
    }

    /**
     * 创建office文档：word excel ppt txt
     *
     * @param officeRequest
     * @return
     */
    @PostMapping("/crud/file/office/create")
    public ResponseEntity<?> createOfficeFile(@RequestBody OfficeRequest officeRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

//        String pid = officeRequest.getPid();
        String fullPath = officeRequest.getFullPath();
        String fileType = officeRequest.getFileType();
        String fileName = officeRequest.getFileName();

        if (!StringUtils.hasLength(fileType) || !StringUtils.hasLength(fileName)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        // 使用 Arrays.asList() 创建一个包含指定字符串的列表
        List<String> validStrings = Arrays.asList("word", "excel", "txt", "ppt", "folder");

        String wordFilePath = "system/office/empty/文字文稿1.docx";
        String excelFilePath = "system/office/empty/工作簿1.xlsx";
        String textFilePath = "system/office/empty/文本文件1.txt";
        String pptFilePath = "system/office/empty/演示文稿1.pptx";

        if (!validStrings.contains(fileType)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UN_QUERY_DATA));
        }
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        try {
            Date date = new Date();
            File fileModel = new File();

            // 判断是否是文件夹
            boolean isFolder = fileType.equals("folder");

            fileName = reFileName(loginUser.getUid(), fullPath, false, fileName, isFolder, timeStamp);

            boolean result = false;

            // 是否是文件夹
            if (!isFolder) {
                String filePath = getFilePath(loginUser.getUid(), fileName, timeStamp);
                String fileUrl = aliyunOssConfig.getOssBucketCdnHost() + "/" + filePath;

                fileModel.setSize(0L);
                if (fileType.equals("word")) {
                    result = storageService.copy(wordFilePath, filePath);
                    fileModel.setSize(10051L);
                }
                if (fileType.equals("excel")) {
                    result = storageService.copy(excelFilePath, filePath);
                    fileModel.setSize(8967L);
                }
                if (fileType.equals("txt")) {
                    result = storageService.copy(textFilePath, filePath);
                    fileModel.setSize(0L);
                }
                if (fileType.equals("ppt")) {
                    result = storageService.copy(pptFilePath, filePath);
                    fileModel.setSize(37131L);
                }
                fileModel.setUrl(fileUrl);
                fileModel.setPath(filePath);
            } else {
                fileModel.setSize(0L);
            }

            if (result || isFolder) {
                File file = fileService.queryFileByFullPath(loginUser.getUid(), fullPath);
                if (file != null) {
                    fileModel.setPid(file.getId());
                }

                String _fullPath = isFolder ? fullPath + fileName + "/" : fullPath + fileName;
                fileModel.setFullPath(_fullPath);

                fileModel.setUid(loginUser.getUid());
                fileModel.setName(fileName);
                fileModel.setType(fileType);
                fileModel.setIsPrivate(false);
                fileModel.setIsDelete(false);
                fileModel.setCreatedDt(date);
                fileModel.setUpdatedDt(date);
                File resultFile = fileService.add2(fileModel);

                return ResponseEntity.ok(new ResultSuccess<>(resultFile));
            } else {
                return ResponseEntity.ok(new ResultSuccess<>(false));
            }

        } catch (Exception ex) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/downloadZipFile")
    public Mono<Void> downloadZipFile(@RequestBody FileRequest fileRequest, ServerHttpResponse response) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            // 直接返回错误响应
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.writeWith(Mono.just(response.bufferFactory()
                    .wrap(ResultStatus.USER_NOT_FOUND.getMessage().getBytes(StandardCharsets.UTF_8))));
        }

        List<File> files = fileRequest.getFiles();
        String downloadFileName = fileRequest.getDownloadFileName();

        if (files == null || !StringUtils.hasLength(downloadFileName)) {
            // 返回空数据错误信息
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return response.writeWith(Mono.just(response.bufferFactory()
                    .wrap(ResultStatus.DATA_EMPTY.getMessage().getBytes(StandardCharsets.UTF_8))));
        }

        return Mono.create(sink -> {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                 ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {

                // 处理所有根目录的文件和文件夹
                for (File rootRecord : files) {
                    compressFolderOrFile(loginUser, rootRecord, "", zipOut);
                }

                zipOut.finish();

                // 设置响应头
                response.setStatusCode(HttpStatus.OK);
                response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileRequest.getDownloadFileName() + ".zip");
                response.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);

                // 将生成的 ZIP 内容写入响应
                DataBuffer buffer = response.bufferFactory().wrap(byteArrayOutputStream.toByteArray());

                // 使用 response.writeWith() 输出流数据
                response.writeWith(Mono.just(buffer)).doOnTerminate(sink::success).subscribe();

            } catch (IOException ex) {
                sink.error(ex);
                log.error("Error occurred while compressing file: {}", ex.getMessage(), ex);
            }
        });
    }

    // 递归压缩虚拟文件夹结构
    private void compressFolderOrFile(LoginUser loginUser, File fileRecord, String parentPath, ZipOutputStream zipOut) throws IOException {
        if (fileRecord.getType().equals("folder")) {
            String folderPath = parentPath + fileRecord.getName() + "/";
            zipOut.putNextEntry(new ZipEntry(folderPath));  // 创建文件夹条目
            zipOut.closeEntry();

            // 查询文件夹下的所有文件和文件夹
            List<File> res = fileService.queryFileListByFullPath(loginUser.getUid(), fileRecord.getFullPath());
            if (!res.isEmpty()) {
                // 获取该文件夹下的子文件和子文件夹
                for (File childRecord : res) {
                    compressFolderOrFile(loginUser, childRecord, folderPath, zipOut);
                }
            }
        } else {
            // 处理文件，将文件内容下载并写入 ZIP
            String fileUrl = fileRecord.getUrl();
            if (StringUtils.hasLength(fileUrl)) {
                compressFileFromUrl(fileRecord.getName(), parentPath, fileUrl, zipOut);
            }
        }
    }

    // 从URL下载文件并写入ZIP中
    private void compressFileFromUrl(String fileName, String parentFolder, String fileUrl, ZipOutputStream zipOut) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) { // 只在状态码 200 时压缩文件
            try (InputStream inputStream = connection.getInputStream()) {
                zipOut.putNextEntry(new ZipEntry(parentFolder + fileName));
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, len);
                }
                zipOut.closeEntry();
            }
        } else {
            log.error("Failed to download file: {}, response code: {}", fileUrl, responseCode);
            throw new IOException("Failed to download file from " + fileUrl);
        }
    }


    /**
     * 上传文件到OSS
     * 注意：1、如果有同名文件，则创建新文件并删除之前的同名文件
     * 2、oss不会保存同名文件，如果存在相同文件则替换
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/add")
    public ResponseEntity<?> addFile(@RequestBody FileRequest fileRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

//        String cid = fileRequest.getCategoryId();
        String fullPath = fileRequest.getFullPath();
        String fileName = fileRequest.getFileName();
        String filePath = fileRequest.getFilePath();
        Long fileSize = fileRequest.getFileSize();
        String type = fileRequest.getFileContentType();

        if (!StringUtils.hasLength(fileName) || !StringUtils.hasLength(filePath) || !StringUtils.hasLength(type)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        try {
            String fileUrl = aliyunOssConfig.getOssBucketCdnHost() + "/" + filePath;

            log.info("fullPath = {}", fullPath);
            log.info("fileName = {}", fileName);

            boolean isExist = fileService.searchFolderPathIsExistFile(loginUser.getUid(), fullPath, fileName, false);

            log.info("是否存在相同文件名：{}", isExist);
            String newFilePath = filePath;
            if (isExist) {
                fileName = reFileName(fileName, false, timeStamp);
                log.info("新的文件名：{}", fileName);
                // 如果存在则重新命名文件名
                newFilePath = reFilePath(filePath, false, timeStamp);
                log.info("新的路径：{}", newFilePath);
                fileUrl = aliyunOssConfig.getOssBucketCdnHost() + "/" + newFilePath;
                log.info("新的url：{}", fileUrl);
                // 不要忘了给oss中的文件也要重命名
                boolean result = storageService.rename(filePath, newFilePath);
                if (!result) {
                    // 保存到oss失败
                    return ResponseEntity.ok(new ResultSuccess<>(false));
                }
            }

            if (StringUtils.hasLength(fileUrl)) {
                Date date = new Date();
                File fileModel = new File();
                fileModel.setUid(loginUser.getUid());

                // 是否有前缀路径，如果有则加上前缀路径
                if (StringUtils.hasLength(fullPath) && !fullPath.equals("/")) {
                    File file = fileService.queryFileByFullPath(loginUser.getUid(), fullPath);
                    if (file != null) {
                        fileModel.setPid(file.getId());
                    }
                    fileModel.setFullPath(fullPath + fileName);
                } else {
                    fileModel.setFullPath("/" + fileName);
                }
                fileModel.setName(fileName);
                fileModel.setType(type);
                fileModel.setSize(fileSize);
                fileModel.setUrl(fileUrl);
                fileModel.setPath(newFilePath);
                fileModel.setIsPrivate(false);
                fileModel.setIsDelete(false);
                fileModel.setCreatedDt(date);
                fileModel.setUpdatedDt(date);
                fileService.add(fileModel);
            }
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.error("uploadLargeFile error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     * 保存生成的二维码
     *
     * @param fileRequest
     * @return
     * @throws IOException
     */
    @PostMapping("/crud/file/addFileForQrcode")
    public ResponseEntity<?> addFileForQrcode(@RequestBody FileRequest fileRequest) throws IOException {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String pid = fileRequest.getPid();
        String qrcodeImg = fileRequest.getQrcodeImg();
        String fileName = fileRequest.getFileName();

        if (!StringUtils.hasLength(pid) || !StringUtils.hasLength(qrcodeImg) || !StringUtils.hasLength(fileName)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            // 将Base64字符串解码为字节数组
            byte[] decodedBytes = Base64.getDecoder().decode(qrcodeImg);
            Long size = (long) decodedBytes.length;
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            // 使用字节数组创建一个ByteArrayInputStream
            InputStream inputStream = new ByteArrayInputStream(decodedBytes);
            fileName = fileName + ".png";

            File parentFile = fileService.queryFileById(pid);
            if (parentFile == null) {
                fileName = reFileName(loginUser.getUid(), "/", true, fileName, false, timeStamp);
            } else {
                fileName = reFileName(loginUser.getUid(), parentFile.getFullPath(), true, fileName, false, timeStamp);
            }

            String imgPath = getFilePath(loginUser.getUid(), fileName, timeStamp);

            // 不能使用用户名，可以换成id
//            String imgPath = "space/file/" + loginUser.getUid() + "/" + System.currentTimeMillis() + "/" + name;

            String imgUrl = storageService.uploadFile(inputStream, imgPath);
            log.info("imgPath={}", imgPath);
            log.info("imgUrl={}", imgUrl);
            if (StringUtils.hasLength(imgUrl)) {
                // 查询同名文件并删除
//                fileService.findFileAndDelete(loginUser.getUid(), filename);
                Date date = new Date();
                File fileModel = new File();
                fileModel.setUid(loginUser.getUid());
                if (!pid.equals("-1")) {
                    fileModel.setPid(pid);
                }
                if (parentFile == null) {
                    fileModel.setFullPath("/" + fileName);
                } else {
                    fileModel.setFullPath(parentFile.getFullPath() + fileName);
                }
                fileModel.setName(fileName);
                fileModel.setType("image");
                fileModel.setSize(size);
                fileModel.setUrl(imgUrl);
                fileModel.setPath(imgPath);
                fileModel.setIsPrivate(false);
                fileModel.setIsDelete(false);
                fileModel.setCreatedDt(date);
                fileModel.setUpdatedDt(date);
                fileService.add(fileModel);
                return ResponseEntity.ok(new ResultSuccess<>(fileModel));
            }
            return ResponseEntity.ok(new ResultSuccess<>(false));
        } catch (Exception ex) {
            log.error("uploadLargeFile error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     * @param fileRequest
     * @return
     * @throws IOException
     */
    @PostMapping("/crud/file/addFileForText")
    public ResponseEntity<?> addFileForText(@RequestBody FileRequest fileRequest) throws IOException {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String pid = fileRequest.getPid();
        String fileName = fileRequest.getFileName();
        String content = fileRequest.getFileContent();
        String type = fileRequest.getFileType();

        if (!StringUtils.hasLength(pid) || !StringUtils.hasLength(content) || !StringUtils.hasLength(type) || !StringUtils.hasLength(fileName)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        try {
            // 查询是否存在相同文件(注意：网盘里面的路径对应的文件肯定是唯一的，所以只能从数据库端判断是否存在相同的文件)
            File parentFile = fileService.queryFileById(pid);
            if (parentFile == null) {
                fileName = reFileName(loginUser.getUid(), "/", true, fileName, false, timeStamp);
            } else {
                fileName = reFileName(loginUser.getUid(), parentFile.getFullPath(), true, fileName, false, timeStamp);
            }

            // (文本生成文件)将 JSON 字符串转换为字节数组
            byte[] byteArray = content.getBytes();

            // 创建 InputStream 来读取字节数组
            InputStream inputStream = new ByteArrayInputStream(byteArray);

            String filePath = getFilePath(loginUser.getUid(), fileName, timeStamp);


            // 不能使用用户名，可以换成id
//            String filePath = "space/file/" + loginUser.getUid() + "/" + System.currentTimeMillis() + "/" + filename;

            String fileUrl = storageService.uploadFile(inputStream, filePath);
            log.info("filePath={}", filePath);
            log.info("fileUrl={}", fileUrl);
            if (StringUtils.hasLength(fileUrl)) {
                // 查询同名文件并删除
//                fileService.findFileAndDelete(loginUser.getUid(), filename);
                Date date = new Date();
                File fileModel = new File();
                fileModel.setUid(loginUser.getUid());
                if (!pid.equals("-1")) {
                    fileModel.setPid(pid);
                }
                if (parentFile == null) {
                    fileModel.setFullPath("/" + fileName);
                } else {
                    fileModel.setFullPath(parentFile.getFullPath() + fileName);
                }

                fileModel.setName(fileName);
                fileModel.setType(type);
                fileModel.setSize((long) byteArray.length);
                fileModel.setUrl(fileUrl);
                fileModel.setPath(filePath);
                fileModel.setIsPrivate(false);
                fileModel.setIsDelete(false);
                fileModel.setCreatedDt(date);
                fileModel.setUpdatedDt(date);
                fileService.add(fileModel);
                // 记得使用完后关闭 InputStream
                inputStream.close();
                return ResponseEntity.ok(new ResultSuccess<>(fileModel));
            }
            // 记得使用完后关闭 InputStream
            inputStream.close();
            return ResponseEntity.ok(new ResultSuccess<>(false));
        } catch (Exception ex) {
            log.error("uploadLargeFile error ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultSuccess<>(false));
        }
    }

    /**
     * 给二维码图片添加边框提高解析
     *
     * @param image
     * @param borderWidth
     * @return
     */
    public static BufferedImage addBorder(BufferedImage image, int borderWidth) {
        int width = image.getWidth() + 2 * borderWidth;
        int height = image.getHeight() + 2 * borderWidth;
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = newImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(image, borderWidth, borderWidth, null);
        g.dispose();

        return newImage;
    }

    /**
     * 上传文件并解析二维码内容
     *
     * @param file
     * @param pid
     * @param filename
     * @return
     * @throws IOException
     */
    @PostMapping("/crud/file/addFileForQrcodeAndRead")
    public Mono<ResponseEntity<?>> addFileForQrcodeAndRead(@RequestPart("file") Mono<FilePart> file,
                                                           @RequestPart("pid") String pid,
                                                           @RequestPart("fileName") String filename) throws IOException {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
        }
        log.info("file={}", file);
        log.info("pid={}", pid);
        log.info("filename={}", filename);

        if (!StringUtils.hasLength(pid) || !StringUtils.hasLength(filename)) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
//        return ResponseEntity.ok(new ResultSuccess<>(false));


        return file.flatMap(fp -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            return fp.content().collectList().flatMap(dataBuffers -> {

                try {
                    dataBuffers.forEach(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        baos.write(bytes, 0, bytes.length);
                    });
                    byte[] bytes = baos.toByteArray();
                    Long size = (long) bytes.length;
                    log.info("size={}", size);
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                    BufferedImage bufferedImage = ImageIO.read(inputStream);

                    BufferedImage bufferedImage2 = addBorder(bufferedImage, 20);

                    LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage2);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    Result result = new MultiFormatReader().decode(bitmap);
                    String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

                    // 注意：重置指针，方便第2次读取，否则下面获取不到inputStream
                    inputStream.reset();
                    // 是否能检测到数据，不能检测数据就添加不成功
                    if (StringUtils.hasLength(result.getText())) {

                        String fileName = filename;

                        // 查询是否存在相同文件(注意：网盘里面的路径对应的文件肯定是唯一的，所以只能从数据库端判断是否存在相同的文件)
                        File parentFile = fileService.queryFileById(pid);
                        if (parentFile == null) {
                            fileName = reFileName(loginUser.getUid(), "/", true, fileName, false, timeStamp);
                        } else {
                            fileName = reFileName(loginUser.getUid(), parentFile.getFullPath(), true, fileName, false, timeStamp);
                        }
                        String imgPath = getFilePath(loginUser.getUid(), fileName, timeStamp);
                        String imgUrl = storageService.uploadFile(inputStream, imgPath);
                        log.info("img url ={}", imgUrl);
                        if (StringUtils.hasLength(imgUrl)) {
                            // 查询同名文件并删除
//                            fileService.findFileAndDelete(loginUser.getUid(), filename);
                            Date date = new Date();
                            File fileModel = new File();
                            fileModel.setUid(loginUser.getUid());
                            if (!pid.equals("-1")) {
                                fileModel.setPid(pid);
                            }
                            if (parentFile == null) {
                                fileModel.setFullPath("/" + fileName);
                            } else {
                                fileModel.setFullPath(parentFile.getFullPath() + fileName);
                            }

                            fileModel.setName(fileName);
                            fileModel.setType("image");
                            fileModel.setSize(size);
                            fileModel.setUrl(imgUrl);
                            fileModel.setPath(imgPath);
                            fileModel.setIsPrivate(false);
                            fileModel.setIsDelete(false);
                            fileModel.setCreatedDt(date);
                            fileModel.setUpdatedDt(date);
                            fileService.add(fileModel);
                        }
                    }
                    // 如何二维码解析出来了，但是添加和上传图片失败，那也返回解析结果
                    return Mono.just(ResponseEntity.ok(new ResultSuccess<>(result)));
                } catch (Exception ex) {
                    log.info("uploadFileAndRead ex={}", ex.getMessage());
                    return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
                }
            });
        });
    }

    /**
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/addForImageAi")
    public ResponseEntity<?> addForImageAi(@RequestBody FileRequest fileRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String pid = fileRequest.getPid();
//        String fileName = fileRequest.getFileName();
        String imageAiId = fileRequest.getImageAiId();

        if (!StringUtils.hasLength(pid) || !StringUtils.hasLength(imageAiId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        ImageAi imageAi = imageAiService.queryByImageAiId(imageAiId);
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        try {
            if (imageAi != null) {
                Date date = new Date();
                File fileModel = new File();
                String fileName = imageAi.getName();
                // 查询是否存在相同文件(注意：网盘里面的路径对应的文件肯定是唯一的，所以只能从数据库端判断是否存在相同的文件)
                File parentFile = fileService.queryFileById(pid);
                if (parentFile == null) {
                    fileName = reFileName(loginUser.getUid(), "/", true, fileName, false, timeStamp);
                    fileModel.setFullPath("/" + fileName);
                } else {
                    fileName = reFileName(loginUser.getUid(), parentFile.getFullPath(), true, fileName, false, timeStamp);
                    fileModel.setFullPath(parentFile.getFullPath() + fileName);
                }

                fileModel.setUid(loginUser.getUid());
                if (!pid.equals("-1")) {
                    fileModel.setPid(pid);
                }

                fileModel.setName(fileName);
                fileModel.setType("image");
                fileModel.setSize(imageAi.getSize());
                fileModel.setUrl(imageAi.getImageSrc());
                fileModel.setPath(imageAi.getImagePath());
                fileModel.setIsPrivate(false);
                fileModel.setIsDelete(false);
                fileModel.setCreatedDt(date);
                fileModel.setUpdatedDt(date);
                fileService.add(fileModel);
                return ResponseEntity.ok(new ResultSuccess<>(true));
            }
        } catch (Exception ex) {
            log.error("uploadLargeFile error ={}", ex.getMessage());
        }
        return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
    }

//    /**
//     * @param cid
//     * @return
//     */
//    @GetMapping("/crud/file/getListByCid")
//    public ResponseEntity<?> queryFilesByCid(@RequestParam String cid) {
//        try {
//            LoginUser loginUser = UserContextHolder.getContext().getUser();
//            if (loginUser == null) {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
//            }
//            if (!StringUtils.hasLength(cid)) {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//            }
//            List<File> fileList = fileService.queryFileByCategoryId(cid);
//
//            return ResponseEntity.ok(new ResultSuccess<>(fileList));
//        } catch (Exception ex) {
//            log.error("queryFilesByCid exception={}", ex.getMessage());
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
//        }
//    }

    @GetMapping("/crud/file/getFileListByPid")
    public ResponseEntity<?> getFileListByPid(@RequestParam String pid) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(pid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            List<File> fileList = fileService.queryFileByPid(pid, loginUser.getUid());

            return ResponseEntity.ok(new ResultSuccess<>(fileList));
        } catch (Exception ex) {
            log.error("queryFilesByCid exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param type
     * @return
     */
    @GetMapping("/crud/file/getListBy")
    public ResponseEntity<?> getListByType(@RequestParam String type, @RequestParam String fullPath) {

        log.info("------------getListByType={}", fullPath);
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(type) || !StringUtils.hasLength("fullPath")) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            //我的文件
            if (type.equals("personal")) {
                List<File> fileList = fileService.queryFileListByFullPath(loginUser.getUid(), fullPath);
                return ResponseEntity.ok(new ResultSuccess<>(fileList));
            }
            // 我分享的
            if (type.equals("sharingin")) {
                if (!fullPath.equals("/")) {
                    List<File> fileList = fileService.queryFileListByFullPath(loginUser.getUid(), fullPath);
                    return ResponseEntity.ok(new ResultSuccess<>(fileList));
                } else {
                    /// 我分享的
                    List<File> fileList = fileService.queryMyShareFileList(loginUser.getUid());
                    return ResponseEntity.ok(new ResultSuccess<>(fileList));
                }
            }
            // 分享给我的
            if (type.equals("sharingout")) {
                if (!fullPath.equals("/")) {
                    List<File> fileList = fileService.queryFileListByFullPath(loginUser.getUid(), fullPath);
                    return ResponseEntity.ok(new ResultSuccess<>(fileList));
                } else {
                    /// 查询分享给我的
                    List<File> fileList = fileService.queryShareFileToMe(loginUser.getUid());
                    return ResponseEntity.ok(new ResultSuccess<>(fileList));
                }
            }
            // 最近
            if (type.equals("recent")) {
                List<File> fileList = fileService.queryFileListByTwoWeeks(loginUser.getUid(), fullPath);
                return ResponseEntity.ok(new ResultSuccess<>(fileList));
            }
            //收藏
            if (type.equals("favorites")) {
                List<File> fileList;
                if (!fullPath.equals("/")) {
                    // 如果是有值，则走这里
                    fileList = fileService.queryFileListByFullPath(loginUser.getUid(), fullPath);
                } else {
                    // 如果是空或者/则走这里
                    fileList = fileService.queryFileListByFavorites(loginUser.getUid());
                }
                return ResponseEntity.ok(new ResultSuccess<>(fileList));
            }
            //回收站
            if (type.equals("trashbin")) {
                List<File> fileList;
                if (!fullPath.equals("/")) {
                    // 如果是有值，则走这里
                    fileList = fileService.queryFileListByFullPath(loginUser.getUid(), fullPath, true);
                } else {
                    fileList = fileService.queryDeleteFileListByOneMonth(loginUser.getUid());
                }
                return ResponseEntity.ok(new ResultSuccess<>(fileList));
            }
//            if (type.equals("folder")) {
//                List<File> fileList = fileService.queryFileListByFolder(loginUser.getUid());
//                return ResponseEntity.ok(new ResultSuccess<>(fileList));
//            }
            //回收站
            if (type.equals("image") || type.equals("document") || type.equals("video") || type.equals("audio")|| type.equals("others")) {
                List<File> fileList = fileService.queryFileListByType(loginUser.getUid(), type);
                return ResponseEntity.ok(new ResultSuccess<>(fileList));
            }

            return ResponseEntity.ok(new ResultSuccess<>(null));
        } catch (Exception ex) {
            log.error("queryFilesByCid exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 根据fileId查询文件
     *
     * @param fileId
     * @return
     */
    @GetMapping("/crud/file/queryFileById")
    public ResponseEntity<?> queryFileById(@RequestParam String fileId) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(fileId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            File result = fileService.queryFileById(fileId);

            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("queryFileById exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 编辑文件名
     *
     * @param fileRequest
     * @return
     */
    @PutMapping("/crud/file/editName")
    public ResponseEntity<?> edit(@RequestBody FileRequest fileRequest) throws IOException {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String newFileName = fileRequest.getFileName();
            String fileId = fileRequest.getFileId();
            if (!StringUtils.hasLength(newFileName) || !StringUtils.hasLength(fileId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            File file = fileService.queryFileById(fileId);
            log.info("file={}", JSONArray.toJSONString(file));
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            String prevPath = getPrevPath(file.getFullPath());

            // 判断是否是文件夹
            boolean isFolder = file.getType().equals("folder");

            // 排除编辑前和编辑后文件名相同的情况，否则，则去查询是否存在相同文件名，如果找到则重命名
            if (!file.getName().equals(newFileName)) {
                log.info("我要重新命名了");
                // 查询是否存在相同文件(注意：网盘里面的路径对应的文件肯定是唯一的，所以只能从数据库端判断是否存在相同的文件)
                boolean isExist = fileService.searchFolderPathIsExistFile(loginUser.getUid(), prevPath, newFileName, isFolder);
                if (isExist) {
                    // 如果存在则重新命名文件名
                    newFileName = reFileName(newFileName, isFolder, timeStamp);
                    log.info("这是我的新名字：{}", newFileName);
                }
            } else {
                // 既然编辑前后文件名相同，那么没有修改的必要
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.FILE_NAME_EXIST));
            }

            String newFullPath = isFolder ? prevPath + newFileName + "/" : prevPath + newFileName;
            log.info("新的fullpath={}", newFullPath);

            if (isFolder) {
                boolean r = fileService.editFolderName(fileId, newFileName, file.getFullPath(), newFullPath);
                return ResponseEntity.ok(new ResultSuccess<>(r));
            } else {
                // 获取当前时间戳
//                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                // 生成UUID
                String uuid = UUID.randomUUID().toString();


                String oldFilePath = file.getPath();
                String newFilePath = "space/file/" + loginUser.getUid() + "/" + timeStamp + "/" + uuid.replace("-", "_") + "/" + newFileName;

                log.info("命名前路径：{}", oldFilePath);
                log.info("命名后路径：{}", newFilePath);
//            String oldFilePath = "space/file/" + file.getUid() + "/" + file.getName();
//            String newFilePath = "space/file/" + file.getUid() + "/" + fileName;
                String fileUrl = aliyunOssConfig.getOssBucketCdnHost() + "/" + newFilePath;
                boolean result = storageService.rename(oldFilePath, newFilePath);
                if (result) {
                    boolean r = fileService.editFileName(fileId, newFileName, fileUrl, newFilePath, newFullPath);
                    return ResponseEntity.ok(new ResultSuccess<>(r));
                } else {
                    return ResponseEntity.ok(new ResultSuccess<>(ResultStatus.UPDATE_ERROR));
                }
            }


        } catch (Exception ex) {
            log.error("edit exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 移动文件
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/move")
    public ResponseEntity<?> move(@RequestBody FileRequest fileRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
//        String[] fileIds = fileRequest.getFileIds();
        // 移动到的文件夹id
        String fileId = fileRequest.getFileId();
        // 被移动的文件
        List<File> files = fileRequest.getFiles();

        if (files == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {

            // 先判断有没有fileId,-1是前端伪造的，代表是根目录
            if (fileId.equals("-1")) {
                for (File val : files) {
                    if (val.getType().equals("folder")) {
                        // 如果移动了文件夹则把该文件夹下所有的文件和子文件夹的fullPath全部修改
                        fileService.moveFolder(val.getId(), "", val.getFullPath(), "/" + val.getName() + "/");
                        fileService.move(val.getId(), "", "/" + val.getName() + "/");
                    } else {
                        // 如果移动了文件夹则把该文件夹下所有的文件和子文件夹的fullPath全部修改
                        fileService.move(val.getId(), "", "/" + val.getName());
                    }
                }
            } else {
                // 有的话查询对应的文件
                File queryFile = fileService.queryFileById(fileId);
                if (queryFile == null) {
                    return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
                }
                for (File val : files) {
                    if (val.getType().equals("folder")) {
                        // 如果移动了文件夹则把该文件夹下所有的文件和子文件夹的fullPath全部修改
                        fileService.moveFolder(val.getId(), fileId, val.getFullPath(), queryFile.getFullPath() + val.getName() + "/");
                        fileService.move(val.getId(), fileId, queryFile.getFullPath() + val.getName() + "/");
                    } else {
                        fileService.move(val.getId(), fileId, queryFile.getFullPath() + val.getName());
                    }
                }
            }
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.error("move files error = {}", ex);
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 删除
     * <p>
     * 注意：不管是删除文件还是文件夹都要把fullPath设置成/文件名形式，也就是设置成/文件名
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/delete")
    public ResponseEntity<?> delete(@RequestBody FileRequest fileRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
//        String[] fileIds = fileRequest.getFileIds();
        List<File> files = fileRequest.getFiles();
        String deleteType = fileRequest.getDeleteType();

        if (files == null || !StringUtils.hasLength(deleteType)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            for (File val : files) {
                if (val.getType().equals("folder")) {
                    log.info("delete file ={}", JSONArray.toJSONString(val));
                    fileService.deleteFolder(val.getFullPath(), deleteType.equals("thoroughly"));
                }
                fileService.delete(val.getId(), deleteType.equals("thoroughly"));
            }
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.error("delete files error = {}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DELETE_ERROR));
        }
    }


    /**
     * 获取文件路径前缀
     * /a.txt       返回 /
     * /a/b.txt     返回 /a/
     * <p>
     * /a/          返回 /
     * /a/b/        返回 /a/
     * /a/b/c/      返回 /a/b/
     *
     * @param fullPath
     * @return
     */
    public String getPrevPath(String fullPath) {
//        String path = file.getFullPath();
        // 去掉末尾的 '/'（如果有）
        if (fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }

        // 找到最后一个 '/' 的索引
        int lastSlashIndex = fullPath.lastIndexOf("/");

        // 如果没有找到 '/'，表示输入可能是文件名（无路径），返回根路径
        if (lastSlashIndex == -1) {
            return "/";
        }

        // 截取到最后一个 '/' 的前面部分
        String parentPath = fullPath.substring(0, lastSlashIndex);

        // 如果 parentPath 为空，说明原路径为根路径，返回 "/"
        if (parentPath.isEmpty()) {
            return "/";
        }

        // 返回结果，确保以 '/' 结尾
        return parentPath + "/";
    }

    /**
     * 恢复删除的文件
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/recovery")
    public ResponseEntity<?> recovery(@RequestBody FileRequest fileRequest) {
        ObjectMapper mapper = new ObjectMapper();

        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        List<File> files = fileRequest.getFiles();

        if (files == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
//         1、判断恢复的prevPath(排除文件名自身的路径)是否存在
//          1.1 如果prevPath有值，并且搜索到prevPath
//              1.1.1 搜索fullPath，有相同的文件/文件夹名，则重新命名，name=new_name，fullPath=fullPath/new_name，isDelete = false，isTrashbin = true，updateDt=new Date()，并且批量搜索并修改所有子文件fullPath=fullPath/new_name
//              1.1.2 没有相同的文件/文件夹名，直接修改isDelete = false，isTrashbin = true，updateDt=new Date()

//          1.2 如果prevPath没有值，还原到根目录
//              1.2.1 判断根目录是否存在该文件/文件夹名，如果存在，则重新命名，name=new_name，fullPath=fullPath/new_name，isDelete = false，isTrashbin = true，updateDt=new Date()
//                  ，并且批量搜索并修改所有子文件fullPath=fullPath/new_name，删除pid
//              1.2.2 不存在相同的文件/文件夹名，直接修改isRealDelete = false，isDelete = false，isTrashbin = true，updateDt=new Date()，删除pid

//          1.3 如果prevPath有值，但是搜索不到prevPath
//              1.3.1 在根目录中搜索/(文件/文件名)，有相同的(文件/文件夹名)，则重新命名，name=new_name，fullPath=fullPath/new_name，isDelete = false，isTrashbin = true，updateDt=new Date()
//                  ，并且批量搜索并修改所有子文件fullPath=fullPath/new_name，删除pid
//              1.3.2 根目录中不存在相同的文件/文件夹名，直接修改isDelete = false，isTrashbin = true，updateDt=new Date()，删除pid

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            log.info("files length={}", files.size());
            for (File oldFile : files) {
                log.info("oldFile ={}", JSONArray.toJSONString(oldFile));
                // 查询完整路径是否已经存在如：/a.txt，/a/c.txt
                boolean result = fileService.isExistFullPath(loginUser.getUid(), oldFile.getFullPath());
                log.info("isExistFullPath = {}", result);

                String prevPath = getPrevPath(oldFile.getFullPath());
                log.info("prevPath = {}", prevPath);
                //1.1
                if (StringUtils.hasLength(prevPath) && !prevPath.equals("/")) {
                    // 说明有父目录
                    boolean isExistPrevPath = fileService.isExistFullPath(loginUser.getUid(), prevPath);
                    log.info("isExistPrevPath ={}", isExistPrevPath);
                    // 1.1
                    if (isExistPrevPath) {
                        // 1.1.1
                        if (result) {
                            log.info("1.1.1 oldFile ={}", JSONArray.toJSONString(oldFile));

                            File newFile = new File();
                            newFile.setType(oldFile.getType());
                            // 如果存在则重新命名文件名
                            String newFileName = reFileName(oldFile.getName(), oldFile.getType().equals("folder"), timeStamp);
                            newFile.setName(newFileName);
                            if (!oldFile.getType().equals("folder")) {
                                String newFilePath = reFilePath(oldFile.getPath(), false, timeStamp);
                                newFile.setPath(newFilePath);
                                String newFileUrl = aliyunOssConfig.getOssBucketCdnHost() + "/" + newFilePath;
                                newFile.setUrl(newFileUrl);
                            }
                            String newFileFullPath = reFileFullPath(oldFile.getFullPath(), oldFile.getType().equals("folder"), timeStamp);
                            newFile.setFullPath(newFileFullPath);
                            log.info("1.1.1 newFile ={}", JSONArray.toJSONString(newFile));
                            fileService.recovery(oldFile, newFile);
                        }
                        // 1.1.2
                        else {
                            log.info("1.1.2 oldFile ={}", JSONArray.toJSONString(oldFile));
                            fileService.recovery(oldFile, oldFile);
                        }
                    } else {
                        boolean result2 = fileService.isExistFullPath(loginUser.getUid(), "/" + oldFile.getName());
                        // 1.3.1
                        if (result2) {
                            log.info("1.3.1 oldFile ={}", JSONArray.toJSONString(oldFile));

                            File newFile = new File();
                            newFile.setType(oldFile.getType());
                            // 如果存在则重新命名文件名
                            String newFileName = reFileName(oldFile.getName(), oldFile.getType().equals("folder"), timeStamp);
                            newFile.setName(newFileName);
                            newFile.setPid("");
                            if (!oldFile.getType().equals("folder")) {
                                String newFilePath = reFilePath(oldFile.getPath(), false, timeStamp);
                                newFile.setPath(newFilePath);
                                String newFileUrl = aliyunOssConfig.getOssBucketCdnHost() + "/" + newFilePath;
                                newFile.setUrl(newFileUrl);
                            }
                            String newFileFullPath = "/" + newFileName;
                            newFile.setFullPath(newFileFullPath);
                            log.info("1.3.1 newFile ={}", JSONArray.toJSONString(newFile));
                            fileService.recovery(oldFile, newFile);
                        }
                        // 1.3.2
                        else {
                            log.info("1.3.2 oldFile ={}", JSONArray.toJSONString(oldFile));

                            File newFile = (File) oldFile.clone();
                            newFile.setFullPath("/" + oldFile.getName() + "/");
                            newFile.setPid("");
                            fileService.recovery(oldFile, newFile);
                        }
                    }
                } else {
                    // 1.2
                    // 说明是根目录
                    // 1.2.1
                    if (result) {
                        log.info("1.2.1 oldFile ={}", JSONArray.toJSONString(oldFile));
                        File newFile = new File();
                        newFile.setType(oldFile.getType());
                        // 如果存在则重新命名文件名
                        String newFileName = reFileName(oldFile.getName(), oldFile.getType().equals("folder"), timeStamp);
                        newFile.setName(newFileName);
                        newFile.setPid("");
                        if (!oldFile.getType().equals("folder")) {
                            String newFilePath = reFilePath(oldFile.getPath(), false, timeStamp);
                            newFile.setPath(newFilePath);
                            String newFileUrl = aliyunOssConfig.getOssBucketCdnHost() + "/" + newFilePath;
                            newFile.setUrl(newFileUrl);
                        }
                        String newFileFullPath = reFileFullPath(oldFile.getFullPath(), oldFile.getType().equals("folder"), timeStamp);
                        newFile.setFullPath(newFileFullPath);
                        log.info("1.2.1 newFile ={}", JSONArray.toJSONString(newFile));
                        fileService.recovery(oldFile, newFile);
                    }
                    // 1.2.2
                    else {
                        log.info("1.2.2 oldFile ={}", JSONArray.toJSONString(oldFile));
                        File newFile = (File) oldFile.clone();
                        newFile.setPid("");
                        fileService.recovery(oldFile, newFile);
                    }
                }
            }
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.info("Exception ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 查询当前文件存储模式，前端据此选择上传通道：
     * local=服务器直传（POST /crud/file/upload），oss=浏览器 OSS STS 直传
     */
    @GetMapping("/crud/file/getUploadMode")
    public ResponseEntity<?> getUploadMode() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        Map<String, String> data = new HashMap<>();
        data.put("mode", storageType);
        return ResponseEntity.ok(new ResultSuccess<>(data));
    }

    /**
     * 文件空间直传接口（本地存储模式）：multipart 上传，流式写盘后落库。
     * 注意：multipart 参数解析在读 body 的 IO 线程上调用本方法，AuthWebFilter 设置的
     * ThreadLocal 用户上下文此时已失效，必须在链内按 token-id 重新解析用户。
     */
    @PostMapping("/crud/file/upload")
    public Mono<ResponseEntity<Object>> uploadFile(ServerHttpRequest request,
                                                   @RequestPart("file") FilePart file,
                                                   @RequestPart("fullPath") String fullPath,
                                                   @RequestPart("fileSize") String fileSizeStr,
                                                   @RequestPart("fileType") String fileType) {
        String tokenId = request.getHeaders().getFirst(UserContext.TOKEN_ID);
        // WebFlux 对非文件 part 只有 String 读取器，Long 必须在业务侧手动解析
        long fileSize;
        try {
            fileSize = Long.parseLong(fileSizeStr);
        } catch (NumberFormatException ex) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
        if (fileSize <= 0 || !StringUtils.hasLength(fileType)) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
        if (fileSize > MAX_UPLOAD_FILE_SIZE) {
            log.warn("uploadFile rejected, size={} exceeds limit {}", fileSize, MAX_UPLOAD_FILE_SIZE);
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
        String folderPath = normalizeFolderPath(fullPath);
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        return tokenSessionService.resolveLoginUser(tokenId)
                .flatMap(loginUser -> Mono.fromCallable(() -> {
                            String fileName = file.filename();
                            boolean isExist = fileService.searchFolderPathIsExistFile(loginUser.getUid(), folderPath, fileName, false);
                            if (isExist) {
                                fileName = reFileName(fileName, false, timeStamp);
                            }
                            String storagePath = getFilePath(loginUser.getUid(), fileName, timeStamp);
                            return new String[]{fileName, storagePath};
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(nameAndPath -> storageService.uploadFilePart(file, nameAndPath[1])
                                .flatMap(url -> Mono.fromCallable(() ->
                                        insertUploadedFile(loginUser.getUid(), folderPath, nameAndPath[0], nameAndPath[1], fileType, fileSize, url)))
                                .map(fm -> okResult(new ResultSuccess<>(fm)))))
                .switchIfEmpty(Mono.just(okResult(new ResultInfo<>(ResultStatus.USER_NOT_FOUND))))
                .onErrorResume(ex -> {
                    log.error("uploadFile error={}", ex.getMessage(), ex);
                    return Mono.just(okResult(false));
                });
    }

    private ResponseEntity<Object> okResult(Object body) {
        return ResponseEntity.ok(body);
    }

    private String normalizeFolderPath(String fullPath) {
        if (!StringUtils.hasLength(fullPath) || "/".equals(fullPath)) {
            return "/";
        }
        return fullPath.endsWith("/") ? fullPath : fullPath + "/";
    }

    private File insertUploadedFile(String uid, String folderPath, String fileName, String storagePath,
                                    String fileType, Long fileSize, String url) throws Exception {
        File fileModel = new File();
        fileModel.setUid(uid);
        if (!"/".equals(folderPath)) {
            File parent = fileService.queryFileByFullPath(uid, folderPath);
            if (parent != null) {
                fileModel.setPid(parent.getId());
            }
            fileModel.setFullPath(folderPath + fileName);
        } else {
            fileModel.setFullPath("/" + fileName);
        }
        fileModel.setName(fileName);
        fileModel.setType(fileType);
        fileModel.setSize(fileSize);
        fileModel.setUrl(url);
        fileModel.setPath(storagePath);
        fileModel.setIsPrivate(false);
        fileModel.setIsDelete(false);
        Date date = new Date();
        fileModel.setCreatedDt(date);
        fileModel.setUpdatedDt(date);
        fileService.add(fileModel);
        return fileModel;
    }

    /**
     * 上传文件前要获取oss sts相关的token信息，参考文章：
     * https://help.aliyun.com/document_detail/100624.htm?spm=a2c4g.11186623.0.0.5a3a5b78cSC50s#concept-xzh-nzk-2gb
     *
     * @return
     */
    @GetMapping("/crud/file/getAcsForUploadFile")
    public ResponseEntity<?> getAcsForUploadFile() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            AssumeRoleResponse acsForUploadFile = ossService.getAcsForUploadFile(loginUser.getUserName());
            if (acsForUploadFile == null || acsForUploadFile.getCredentials() == null) {
                log.error("getAcsForUploadFile failed: null response from STS");
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
            }
            return ResponseEntity.ok(new ResultSuccess<>(acsForUploadFile));
        } catch (Exception ex) {
            log.error("getAcsForUploadFile exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @return
     */
    @GetMapping("/crud/file/getMyFileSumSize")
    public ResponseEntity<?> getMyFileSumSize() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            File result = fileService.getFileSizeByUserId(loginUser.getUid());
            if (result == null) {
                return ResponseEntity.ok(new ResultSuccess<>(0));
            } else {
                return ResponseEntity.ok(new ResultSuccess<>(result.getSize()));
            }
        } catch (Exception ex) {
            log.error("getMyFileSumSize exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 批量设置成是否私有
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/setPrivate")
    public ResponseEntity<?> setPrivate(@RequestBody FileRequest fileRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String[] fileIds = fileRequest.getFileIds();
            Boolean isPrivate = fileRequest.getIsPrivate();
            for (String val : fileIds) {
                fileService.editIsPrivate(val, isPrivate);
            }
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.error("setPrivate exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 查询所有文件夹
     *
     * @return
     */
    @PostMapping("/crud/file/queryFolderList")
    public ResponseEntity<?> queryFolderList(@RequestBody FileRequest fileRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            List<File> files = fileRequest.getFiles();
            if (files == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            // 使用 Stream API 提取所有的 ID
            List<String> ids = files.stream()
                    .map(File::getId)  // 获取每个 File 对象的 ID
                    .toList();  // 收集结果为 List

            List<File> result = fileService.queryFolderList(loginUser.getUid(), ids);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("queryFolderList exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @return
     */
    @PostMapping("/crud/file/queryFolderTreeList")
    public ResponseEntity<?> queryFolderTreeList() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            List<String> list = new ArrayList<>();
            List<File> result = fileService.queryFolderList(loginUser.getUid(), list);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("setPrivate exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 设置是否收藏
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/setFavorite")
    public ResponseEntity<?> setFavorite(@RequestBody FileRequest fileRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            List<File> files = fileRequest.getFiles();

            if (files == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            for (File file : files) {
                if (file.getIsFavorite() != null) {
                    fileService.setFavorite(file.getId(), !file.getIsFavorite());
                } else {
                    fileService.setFavorite(file.getId(), true);
                }

            }
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.info("setFavorite ex={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 添加分享文件的用户
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/shareToFriends")
    public Mono<ResponseEntity<?>> shareToFriends(@RequestBody FileRequest fileRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }

            String fileId = fileRequest.getFileId();
            String fid = fileRequest.getFid();

            if (!StringUtils.hasLength(fileId) || !StringUtils.hasLength(fid)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }

            // 查询被发送消息用户信息
            Mono<User> userMono = usersFacade.getAccountInfoByUseId(fid);
            return userMono.flatMap(user -> {
                log.info("userMono res:{}", JSONArray.toJSONString(user));
                if (user != null) {

                    // 添加分享日期
                    ShareFileToUser shareFileToUser = new ShareFileToUser();
                    // 使用 BeanUtils 将 User 的属性复制到 ShareFileToUser 对象中
                    BeanUtils.copyProperties(user, shareFileToUser);

                    shareFileToUser.setShareDt(new Date());

                    log.info("shareFileToUser ={}", shareFileToUser);

                    // 添加用户friend到文件中
                    boolean result = fileService.addToSetFriend(fileId, shareFileToUser);

                    if (result){
                        // 添加成功再查询该消息是否已经发送过
                        Mono<NotifyRemind> notifyRemindMono = notificationDowngradeService.queryNotifyBy(loginUser, loginUser.getUid(), fileId, fid);
                        File file = fileService.queryFileById(fileId);

                        if (file != null){
                            return notifyRemindMono.flatMap(res -> {
                                log.info("notifyRemindMono res:{}", JSONArray.toJSONString(res));
                                // 没查询到消息，则发送新消息，查询到了说明已经发送过了，所以不用重复发送
                                if (res == null || !StringUtils.hasLength(res.getId())) {
                                    // 发送消息通知
                                }
                                return Mono.just(ResponseEntity.ok(new ResultSuccess<>(true)));
                            });
                        }else{
                            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.UN_QUERY_DATA)));
                        }
                    }
                }
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            });
        } catch (Exception ex) {
            log.info("setFavorite ex={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 获取该文件已分享的用户列表
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/getShareFileFriends")
    public ResponseEntity<?> getShareFileFriends(@RequestBody FileRequest fileRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        String fileId = fileRequest.getFileId();
        if (!StringUtils.hasLength(fileId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            File file = fileService.queryFileById(fileId);
            List<ShareFileToUser> result = file.getShareToFriends();
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.info("getShareFileFriends ex={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除指定的文件分享的用户
     *
     * @param fileRequest
     * @return
     */
    @PostMapping("/crud/file/deleteShareFileFriend")
    public ResponseEntity<?> deleteShareFileFriend(@RequestBody FileRequest fileRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        String fileId = fileRequest.getFileId();
        String fid = fileRequest.getFid();
        if (!StringUtils.hasLength(fid) || !StringUtils.hasLength(fileId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = fileService.deleteFriend(fileId, fid);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.info("getShareFileFriends ex={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param fileId
     * @return
     */
    @GetMapping("/crud/file/getSharedFile")
    public ResponseEntity<?> getSharedFriends(@RequestParam String fileId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(fileId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            File result = fileService.queryFileById(fileId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.info("getShareFileFriends ex={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


//    /**
//     * 发送消息给朋友
//     * @param fileRequest
//     * @return
//     */
//    @PostMapping("/crud/file/sendNotifyToFriends")
//    public ResponseEntity<?> sendNotifyToFriends(@RequestBody FileRequest fileRequest) {
//        LoginUser loginUser = UserContextHolder.getContext().getUser();
//        if (loginUser == null) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
//        }
//        String fileId = fileRequest.getFileId();
//        List<String> fids = fileRequest.getFids();
//
//        if (!StringUtils.hasLength(fileId) || fids.isEmpty()) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//
//        try {
//            File file = fileService.queryFileById(fileId);
//            if (file != null) {
//                Mono<List<NotifyRemind>> listMono = notificationDowngradeService.queryNotifyBy(loginUser, loginUser.getUid(), fileId, fids);
//
//                listMono.subscribe(res -> {
//                    log.info("result:{}", res);
////                    返回为空则全部发送
//                    if (res.isEmpty()) {
//                        log.info("--- 全部发送 ---");
//                        for (String fid : fids) {
//                            sendNotify(loginUser, file, fid);
//                        }
//                    } else {
//                        log.info("--- 部分发送或全部不发送 ---");
//                        // 找出 fids 中不存在于 notifyRemind.id 的值
//                        List<String> missingIds = fids.stream()
//                                .filter(fid -> res.stream().noneMatch(recipient -> recipient.getRecipientId().equals(fid)))
//                                .collect(Collectors.toList());
//                        log.info("--- missingIds --- {}", JSONArray.toJSONString(missingIds));
//                        for (String fid : missingIds) {
//                            sendNotify(loginUser, file, fid);
//                        }
//                    }
//                });
//                return ResponseEntity.ok(new ResultSuccess<>(true));
//            } else {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UN_QUERY_DATA));
//            }
//        } catch (Exception ex) {
//            log.info("sendNotifyToFriends ex ={}", ex.getMessage());
//            return ResponseEntity.ok(new ResultInfo<>(ex.getMessage()));
//        }
//    }

    /**
     * @param loginUser
     * @param file
     * @param user
     */
}
