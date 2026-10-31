package com.seaurl.platform.space.service;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.seaurl.platform.space.model.ChatAiMessage;
import com.seaurl.platform.space.model.ImageAi;
import com.seaurl.platform.space.model.QueryResults2;

import java.util.List;

public interface ImageAiService {


    boolean editImage(String imageAiId, String imageSrc);

    boolean editImage(String imageAiId, String imageSrc, Long size);

    boolean editImage(String imageAiId, String imageSrc, String imagePath, Long size);

    boolean editImage(String imageAiId, String name, String imageSrc, String imagePath, Long size);

    public ImageSynthesisResult basicCall(String prompt, String size, Integer n) throws ApiException, NoApiKeyException;

    ImageAi add(ImageAi imageAi);

    ImageAi queryByImageAiId(String imageAiId);

    QueryResults2 queryMessageByPage(String uid, Integer pageNum, Integer pageSize);

    QueryResults2 queryMessageByPage(String uid, String flagCreatedTime, Integer pageNum, Integer pageSize);

    boolean removeByImageAiId(String uid, String imageAiId);

    boolean removeById(String uid, String id);
}
