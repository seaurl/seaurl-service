package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.KnowledgeCategory;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.request.KnowledgeCategoryRequest;
import com.seaurl.platform.space.service.KnowledgeCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/space")

public class KnowledgeCategoryController {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private KnowledgeCategoryService knowledgeCategoryService;


    /**
     *
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/knowledge/category/create")
    public ResponseEntity<?> create(@RequestBody KnowledgeCategoryRequest categoryRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(categoryRequest.getName())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try{
            Date date = new Date();
            KnowledgeCategory knowledgeCategory = new KnowledgeCategory();
            knowledgeCategory.setName(categoryRequest.getName());
            knowledgeCategory.setDesc(categoryRequest.getDesc());
            knowledgeCategory.setIsDelete(false);

            knowledgeCategory.setCreatedDt(date);
            knowledgeCategory.setUpdatedDt(date);
            KnowledgeCategory result = knowledgeCategoryService.create(knowledgeCategory);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex){
            log.error("create knowledge category exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     *
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/knowledge/category/update")
    public ResponseEntity<?> update(@RequestBody KnowledgeCategoryRequest categoryRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(categoryRequest.getId())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try{
            Date date = new Date();
            KnowledgeCategory knowledgeCategory = new KnowledgeCategory();

            if (StringUtils.hasLength(categoryRequest.getName())){
                knowledgeCategory.setName(categoryRequest.getName());
            }
            if (StringUtils.hasLength(categoryRequest.getDesc())){
                knowledgeCategory.setDesc(categoryRequest.getDesc());
            }

            knowledgeCategory.setUpdatedDt(date);
            Boolean result = knowledgeCategoryService.update(knowledgeCategory);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex){
            log.error("update knowledge category exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     *
     * @return
     */
    @GetMapping("/crud/knowledge/category/query")
    public ResponseEntity<?> query() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        try{
            List<KnowledgeCategory> result = knowledgeCategoryService.getAll(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex){
            log.error("query knowledge category exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     *
     * @param id
     * @return
     */
    @PostMapping("/crud/knowledge/category/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(id)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try{
            //todo:删除前判断有没有文档，如果有，则不能删除
           Boolean result = knowledgeCategoryService.delete(id);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex){
            log.error("query knowledge category exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
