package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.request.CategoryRequest;
import com.seaurl.platform.space.service.FileCategoryService;
import com.seaurl.platform.space.service.FileService;
import com.seaurl.platform.space.service.UserService;
import com.seaurl.platform.space.service.facade.UsersFacade;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/space")

@Observed
public class FileCategoryController {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    UsersFacade usersFacade;

    @Autowired
    UserService userService;

    @Autowired
    FileCategoryService fileCategoryService;

    @Autowired
    FileService fileService;

    /**
     * 添加文件分类
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/fileCategory/add")
    public Mono<ResponseEntity<?>> addCategory(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }
            String name = categoryRequest.getName();
            String pid = categoryRequest.getPid();
            if (!StringUtils.hasLength(name) || !StringUtils.hasLength(pid)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }
            Mono<User> userMono = usersFacade.getAccountInfoByUseId(loginUser.getUid());
            return userMono.map(user -> {
                FileCategory fileCategory = new FileCategory();
                Date current = new Date();
                fileCategory.setName(name);
                fileCategory.setUid(user.getUid());
                fileCategory.setCreatedDt(current);
                fileCategory.setUpdatedDt(current);
                // 默认根节点Pid=0，从前端传回过来
                fileCategory.setPid(pid);
                FileCategory result = fileCategoryService.addCategory2(fileCategory);
                return ResponseEntity.ok(new ResultSuccess<>(result));
            });
        }catch (Exception ex) {
            log.error("addCategory exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }


    /**
     * 编辑文件分类
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/fileCategory/edit")
    public ResponseEntity<?> editCategory(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String id = categoryRequest.getId();
            String name = categoryRequest.getName();
            String pid =  categoryRequest.getPid();
            if (!StringUtils.hasLength(id) || !StringUtils.hasLength(name)|| !StringUtils.hasLength(pid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = fileCategoryService.editCategoryName(id, name, pid);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("editCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除文件分类
     * @param categoryRequest
     * @return
     */
//    @PostMapping("/crud/fileCategory/delete")
//    public ResponseEntity<?> deleteCategory(@RequestBody CategoryRequest categoryRequest) {
//        try {
//            LoginUser loginUser = UserContextHolder.getContext().getUser();
//            if (loginUser == null) {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
//            }
//
//            // categoryId
//            String id = categoryRequest.getId();
//            if (!StringUtils.hasLength(id)) {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//            }
//            //删除时查看底下是否有子分类，如果有，不能删除
//            List<FileCategory> fileCategories = fileCategoryService.queryCategoryByPid(id);
//            if (fileCategories.size() > 0) {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EXISTED));
//            }
//            // 判断是否有笔记，有笔记也不能删除
//            List<File> urls = fileService.queryFileByCategoryId(id);
//            if (urls.size() > 0) {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EXISTED));
//            }
//            boolean result = fileCategoryService.deleteCategory(id);
//            return ResponseEntity.ok(new ResultSuccess<>(result));
//        }catch (Exception ex) {
//            log.error("deleteCategory exception={}", ex.getMessage());
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
//        }
//    }


    /**
     *
     * @param userName
     * @return
     */
    @GetMapping("/crud/fileCategory/categoryTreeList")
    public Mono<ResponseEntity<?>> queryCategoryTreeList() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }
            Mono<User> userMono = usersFacade.getAccountInfoByUseId(loginUser.getUid());
            return userMono.map(user -> {
                List<FileCategory> fileCategories = fileCategoryService.queryCategoryByUid(user.getUid());

                return ResponseEntity.ok(new ResultSuccess<>(fileCategories));
            });
        }catch (Exception ex) {
            log.error("queryCategoryTreeList exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }


    /**
     *
     * @param userName
     * @return
     */
    @GetMapping("/crud/fileCategory")
    public Mono<ResponseEntity<?>> queryCategory() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
            Mono<User> userMono = usersFacade.getAccountInfoByUseId(loginUser.getUid());
            return userMono.map(user -> {
                List<FileCategory> fileCategories = fileCategoryService.queryCategoryByUid(user.getUid());

                return ResponseEntity.ok(new ResultSuccess<>(fileCategories));
            });
        }catch (Exception ex) {
            log.error("queryCategory exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 根据pid查询子分类列表
     *
     * @param pid
     * @return
     */
    @GetMapping("/info/fileCategory/sub/{pid}")
    public ResponseEntity<?> queryCategoriesById(@PathVariable String pid) {
        try {
            if (!StringUtils.hasLength(pid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            List<FileCategory> fileCategories = fileCategoryService.queryCategoryByPid(pid);
            return ResponseEntity.ok(new ResultSuccess<>(fileCategories));
        }catch (Exception ex) {
            log.error("queryCategoriesById exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 设置是否置顶
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/file/category/top")
    public ResponseEntity<?> updateTop(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(categoryRequest.getCid())) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = fileCategoryService.setTop(categoryRequest.getCid(), categoryRequest.getIsTop());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("updateTop exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
