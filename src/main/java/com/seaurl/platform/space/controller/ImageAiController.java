package com.seaurl.platform.space.controller;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisOutput;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.fastjson.JSONArray;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.ImageAi;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.model.QueryResults2;
import com.seaurl.platform.space.request.ChatAiRequest;
import com.seaurl.platform.space.request.ImageAiRequest;
import com.seaurl.platform.space.service.ImageAiService;
import com.seaurl.platform.space.service.StorageService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/space")

public class ImageAiController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    AliyunConfig aliyunConfig;

    @Autowired
    ImageAiService imageAiService;

    @Autowired
    StorageService storageService;

    @Autowired
    AliyunConfig aliyunOssConfig;

    /**
     * 1、将用户上传的图片保存到用户文件夹中，
     * 2、将图片完整路径调用ai接口生成图片
     * 3、返回生成的图片给前端展示
     */

    /**
     * 获取 image ai列表
     *
     * @param imageAiRequest
     * @return
     */
    @PostMapping(value = "/crud/imageAi/getList")
    public ResponseEntity<?> getList(@RequestBody ImageAiRequest imageAiRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String flagCreatedTime = imageAiRequest.getFlagCreatedTime();

        Integer pageNum = imageAiRequest.getPageNum();
        Integer pageSize = imageAiRequest.getPageSize();
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 20;
        }
        log.info("pageNum ={}", pageNum);
        try {
            QueryResults2 result;
            if (StringUtils.hasLength(flagCreatedTime)) {
                log.info("flagCreatedTime ={}", flagCreatedTime);
                result = imageAiService.queryMessageByPage(loginUser.getUid(), flagCreatedTime, pageNum, pageSize);
            } else {
                result = imageAiService.queryMessageByPage(loginUser.getUid(), pageNum, pageSize);
            }
            if (result != null) {
                return ResponseEntity.ok(new ResultSuccess<>(result));
            } else {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.QUERY_ERROR));
            }

        } catch (Exception ex) {
            log.info("getImageAiList error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.QUERY_ERROR));
        }
    }

    /**
     * 删除生成的图片
     *
     * @param imageAiId
     * @return
     */
    @GetMapping(value = "/crud/imageAi/delete/{imageAiId}")
    public ResponseEntity<?> add(@PathVariable String imageAiId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(imageAiId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            boolean result = imageAiService.removeByImageAiId(loginUser.getUid(), imageAiId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.info("error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DELETE_ERROR));
        }
    }

    /**
     * 添加 image ai基础数据
     *
     * @param imageAiRequest
     * @return
     */
    @PostMapping(value = "/crud/imageAi/add")
    public ResponseEntity<?> add(@RequestBody ImageAiRequest imageAiRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        Constants.apiKey = aliyunConfig.getDashscopeApiKey();

        String aiModel = imageAiRequest.getAiModel();
        String prom = imageAiRequest.getPrompt();
        String ratio = imageAiRequest.getRatio();
//        String size = imageAiRequest.getSize();
        Integer n = imageAiRequest.getCount();

        if (n == null) {
            n = 1;
        }

        if (!StringUtils.hasLength(prom) || !StringUtils.hasLength(aiModel)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }


        try {
            Date date = new Date();
            ImageAi imageAi = new ImageAi();
            ObjectId newId = new ObjectId();
            imageAi.setAiModel(aiModel);
            imageAi.setImageAiId(newId.toString());
            imageAi.setCount(n);
            imageAi.setRatio(ratio);
//            imageAi.setSize(size);
            imageAi.setPrompt(prom);
            imageAi.setUid(loginUser.getUid());
            imageAi.setStatus("2");
            imageAi.setCreatedDt(date);
            imageAi.setUpdatedDt(date);

            ImageAi result = imageAiService.add(imageAi);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.info("error={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.ADD_ERROR));
        }
    }


    /**
     * 根据url下载图片并转换成InputStream类型
     *
     * @param imageUrl
     * @return
     * @throws IOException
     */
    public URLConnection downloadImageAsStream(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();

        // 设置超时，防止网络延迟
        connection.setConnectTimeout(5000); // 5秒
        connection.setReadTimeout(5000); // 5秒
        // 获取输入流
        return connection;
    }

    /**
     * 使用通义万相生成图片
     *
     * @return
     */
    @PostMapping(value = "/crud/imageAi/generate")
    public ResponseEntity<?> generate(@RequestBody ImageAiRequest imageAiRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        Constants.apiKey = aliyunConfig.getDashscopeApiKey();

        String imageAiId = imageAiRequest.getImageAiId();

        if (!StringUtils.hasLength(imageAiId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            ImageAi imageAi = imageAiService.queryByImageAiId(imageAiId);
            ImageSynthesisResult result = imageAiService.basicCall(imageAi.getPrompt(), imageAi.getRatio(), imageAi.getCount());
            ImageSynthesisOutput output = result.getOutput();
            log.info("output={}", JSONArray.toJSON(output));
            if (!output.getResults().isEmpty()) {
                if (output.getResults().get(0).containsKey("url")) {

                    // 把图片转换成图片流
                    String imageSrc = output.getResults().get(0).get("url");

                    URLConnection conn = downloadImageAsStream(imageSrc);
                    InputStream inputStream = conn.getInputStream();
                    Long size = conn.getContentLengthLong();

                    Long name = System.currentTimeMillis();

                    // 获取当前时间戳
                    String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                    // 生成UUID
                    String uuid = UUID.randomUUID().toString();
                    String imgPath = "space/file/" + loginUser.getUid() + "/imageAi/" + timeStamp + "/" + uuid.replace("-", "_") + "/" + name + ".png";

                    // 不能使用用户名，可以换成id
//                    String imgPath = "space/file/" + loginUser.getUid() +"/" + System.currentTimeMillis() + "/" + name + ".png";

                    String imgUrl = storageService.uploadFile(inputStream, imgPath);

                    // 将图片上传到个人文件夹中
                    boolean end = imageAiService.editImage(imageAiId, name + ".png", imgUrl, imgPath, size);
                    if (end) {
                        imageAi.setImageSrc(imgUrl);
                        imageAi.setStatus("1");
                        return ResponseEntity.ok(new ResultSuccess<>(imageAi));
                    }
                }
            }
            // 生成失败的话，要删除添加的信息
            imageAiService.removeByImageAiId(loginUser.getUid(), imageAiId);
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.ADD_ERROR));
        } catch (Exception ex) {
            log.info("error={}", ex.getMessage());
            // 生成失败的话，要删除添加的信息
            imageAiService.removeByImageAiId(loginUser.getUid(), imageAiId);
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.ADD_ERROR));
        }
    }
}
