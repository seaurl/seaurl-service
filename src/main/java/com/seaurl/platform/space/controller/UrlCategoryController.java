package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.UrlCategory;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.request.CategoryRequest;
import com.seaurl.platform.space.request.UrlRequest;
import com.seaurl.platform.space.service.UrlCategoryService;
import com.seaurl.platform.space.service.UrlService;
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
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/space")

@Observed
public class UrlCategoryController {
    private static Logger log = LoggerFactory.getLogger(UrlCategoryController.class);

    @Autowired
    UrlCategoryService urlCategoryService;

    @Autowired
    UserService userService;

    @Autowired
    UrlService urlService;

    @Autowired
    UsersFacade usersFacade;

    /**
     *
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/urlCategory/move")
    public ResponseEntity<?> moveCategory(@RequestBody CategoryRequest categoryRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String categoryId = categoryRequest.getId();
        String pid = categoryRequest.getPid();
        Integer sort = categoryRequest.getSort();

        log.info("categoryId={},pid={},sort={}", categoryId, pid, sort);

        if (!StringUtils.hasLength(categoryId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }



        try {
            boolean result  = urlCategoryService.move(categoryId, pid, sort);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex){
            log.error("moveCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     *
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/urlCategory/drag")
    public ResponseEntity<?> dragCategory(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String categoryId = categoryRequest.getId();
            String targetId = categoryRequest.getTargetId();
            String type = categoryRequest.getDragType();
            if (!StringUtils.hasLength(categoryId) || !StringUtils.hasLength(targetId) || !StringUtils.hasLength(type)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = urlCategoryService.dragCategory(loginUser.getUid(), categoryId, targetId, type);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("dragCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/urlCategory/add")
    public ResponseEntity<?> addCategory(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String name = categoryRequest.getName();
            String pid = categoryRequest.getPid();
            if (!StringUtils.hasLength(name) || !StringUtils.hasLength(pid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
//        UrlCategory uc = urlCategoryService.getMaxSortCategory(user.getUid(), "");
            UrlCategory urlCategory = new UrlCategory();
            Date current = new Date();
            urlCategory.setName(name);
            urlCategory.setUid(loginUser.getUid());
//            urlCategory.setIsTop(false);
            urlCategory.setCreatedDt(current);
            urlCategory.setUpdatedDt(current);
            // 默认根节点Pid=0，从前端传回过来
            urlCategory.setPid(pid);
            UrlCategory result = urlCategoryService.addCategory2(urlCategory);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("addCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 添加子分类
     *
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/urlCategory/addSubCategory")
    public ResponseEntity<?> addSubCategory(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String id = categoryRequest.getId();
            String name = categoryRequest.getName();
            if (!StringUtils.hasLength(id) || !StringUtils.hasLength(name)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            UrlCategory uc = urlCategoryService.getMaxSortCategory(loginUser.getUid(), "");
            UrlCategory urlCategory = new UrlCategory();
            urlCategory.setPid(id);
            urlCategory.setUid(loginUser.getUid());
            urlCategory.setName(name);
            if (uc != null) {
                urlCategory.setSort(uc.getSort() + 1);
            } else {
                urlCategory.setSort(1);
            }
            urlCategoryService.addCategory(urlCategory);
            return ResponseEntity.ok(new ResultSuccess<>(true));
        }catch (Exception ex) {
            log.error("addSubCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 编辑分类名称
     *
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/urlCategory/editCategoryName")
    public ResponseEntity<?> editCategory(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String id = categoryRequest.getId();
            String name = categoryRequest.getName();
            String pid = categoryRequest.getPid();
            if (!StringUtils.hasLength(id) || !StringUtils.hasLength(name) || !StringUtils.hasLength(pid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = urlCategoryService.editCategoryName(id, name, pid);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("editCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除分类
     *
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/urlCategory/delete")
    public ResponseEntity<?> deleteCategory(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            // categoryId
            String id = categoryRequest.getId();
            if (!StringUtils.hasLength(id)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            //删除时查看底下是否有子分类，如果有，不能删除
            List<UrlCategory> urlCategories = urlCategoryService.queryCategoryByPid(id);
            if (urlCategories.size() > 0) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EXISTED));
            }
            // 判断是否有网址，有网址也不能删除
            List<Url> urls = urlService.queryUrlByCategoryId(id);
            if (urls.size() > 0) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EXISTED));
            }
            boolean result = urlCategoryService.deleteCategory(id);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("deleteCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 查询分类列表供添加网址选择使用
     *
     * @param userName
     * @return
     */
    @GetMapping("/crud/urlCategory/categoryTreeList")
    public Mono<ResponseEntity<?>> queryCategoryTreeList() {
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
                List<UrlCategory> urlCategoryList = urlCategoryService.queryAllCategoriesByUid(user.getUid());

                return ResponseEntity.ok(new ResultSuccess<>(urlCategoryList));
            });
        }catch (Exception ex) {
            log.error("queryCategoryTreeList exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 根据当前登录用户查询所有分类列表
     *
     * @return
     */
    @GetMapping("/crud/urlCategory")
    public Mono<ResponseEntity<?>> queryCategory() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
            }
            List<HashMap> urlCategoryList = urlCategoryService.queryAllCategoriesAndUrlCountByUid(loginUser.getUid());
            return Mono.just(ResponseEntity.ok(new ResultSuccess<>(urlCategoryList)));
        } catch (Exception ex) {
            log.error("exception is ={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ex.getMessage())));
        }
    }

    /**
     * 根据用户名查询所有分类列表
     *
     * @return
     */
    @GetMapping("/info/urlCategory/{userName}")
    public Mono<ResponseEntity<?>> queryCategoryByUserName(@PathVariable String userName) {
        if (!StringUtils.hasLength(userName)) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
        log.info("user name is ={}", userName);
        try {
            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
            return userMono.map(user -> {
                log.info("user is here ={}", user.getUid());
                List<HashMap> urlCategoryList = urlCategoryService.queryAllCategoriesAndUrlCountByUid(user.getUid());
                return ResponseEntity.ok(new ResultSuccess<>(urlCategoryList));
            });
        } catch (Exception ex) {
            log.error("exception is ={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ex.getMessage())));
        }
    }

    /**
     * 通过cid查询当前节点的pid，再查询所有pid=pid的分类列表
     *
     * @param
     * @return
     */
    @GetMapping("/info/urlCategory/queryCategories/{cid}")
    public ResponseEntity<?> queryCategories(@PathVariable String cid) {
        try {
            if (!StringUtils.hasLength(cid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            UrlCategory urlCategory = urlCategoryService.queryCategoryById(cid);
            if (urlCategory != null) {
                List<UrlCategory> urlCategories = urlCategoryService.queryCategoryByUPid(urlCategory.getUid(), urlCategory.getPid());
                return ResponseEntity.ok(new ResultSuccess<>(urlCategories));
            } else {
                return ResponseEntity.ok(new ResultSuccess<>(null));
            }
        }catch (Exception ex) {
            log.error("queryCategories exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据pid查询子分类列表
     *
     * @param pid
     * @return
     */
    @GetMapping("/info/urlCategory/querySubCategories/{pid}")
    public ResponseEntity<?> queryCategoriesById(@PathVariable String pid) {
        try {
            if (!StringUtils.hasLength(pid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            List<UrlCategory> urlCategoryList = urlCategoryService.queryCategoryByPid(pid);
            return ResponseEntity.ok(new ResultSuccess<>(urlCategoryList));
        }catch (Exception ex) {
            log.error("queryCategoriesById exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据categoryId查询网址列表
     *
     * @param cid
     * @return
     */
    @GetMapping("/info/url/getUrlBy/{cid}")
    public ResponseEntity<?> getUrlByCategoryId(@PathVariable String cid) {
        try {
            if (!StringUtils.hasLength(cid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            List<Url> urls = urlService.queryUrlByCategoryId(cid);
            return ResponseEntity.ok(new ResultSuccess<>(urls));
        }catch (Exception ex) {
            log.error("getUrlByCategoryId exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     *
     * 分页查询网址列表
     *
     * @return
     */
    @PostMapping("/info/url/getUrlByCid")
    public ResponseEntity<?> getUrlByCategoryIdAndPagination(@RequestBody UrlRequest urlRequest) {
        try {
            String cid = urlRequest.getCategoryId();
            Integer pageNum = urlRequest.getPageNum();
            Integer pageSize = urlRequest.getPageSize();
            if (pageNum == null) {
                pageNum = 1;
            }
            if (pageSize == null) {
                pageSize = 60;
            }
            if (!StringUtils.hasLength(cid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            List<HashMap> result = urlService.queryUrlByCategoryId(cid, pageNum, pageSize);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("getUrlByCategoryIdAndPagination exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 设置是否置顶
     *
     * @param categoryRequest
     * @return
     */
    @PostMapping("/crud/url/category/top")
    public ResponseEntity<?> updateTop(@RequestBody CategoryRequest categoryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            if (!StringUtils.hasLength(categoryRequest.getCid())) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = urlCategoryService.setTop(categoryRequest.getCid(), categoryRequest.getIsTop());

            return ResponseEntity.ok(new ResultSuccess<>(result));
        }catch (Exception ex) {
            log.error("updateTop exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
