package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContextHolder;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.model.PasswordCategory;
import com.seaurl.platform.space.model.PasswordEntry;
import com.seaurl.platform.space.request.PasswordRequest;
import com.seaurl.platform.space.service.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/space")
public class PasswordController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    PasswordService passwordService;

    @GetMapping("/crud/password/query")
    public ResponseEntity<?> query() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<PasswordEntry> result = passwordService.queryAll(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password query exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/password/search")
    public ResponseEntity<?> search(@RequestParam(value = "keyword", required = false) String keyword) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<PasswordEntry> result = passwordService.search(loginUser.getUid(), keyword);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password search exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/password/add")
    public ResponseEntity<?> add(@RequestBody PasswordRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getTitle()) || !StringUtils.hasLength(request.getUsername()) || !StringUtils.hasLength(request.getPassword())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            PasswordEntry result = passwordService.add(
                    loginUser.getUid(),
                    request.getTitle(),
                    request.getUsername(),
                    request.getPassword(),
                    request.getUrl(),
                    request.getRepoUrl(),
                    request.getRepoBranch(),
                    request.getNotes(),
                    request.getCategory(),
                    request.getParentId(),
                    request.getColor(),
                    request.getCategoryId());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password add exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/password/update")
    public ResponseEntity<?> update(@RequestBody PasswordRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getId())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = passwordService.update(
                    request.getId(),
                    request.getTitle(),
                    request.getUsername(),
                    request.getPassword(),
                    request.getUrl(),
                    request.getRepoUrl(),
                    request.getRepoBranch(),
                    request.getNotes(),
                    request.getCategory(),
                    request.getParentId(),
                    request.getColor(),
                    request.getCategoryId());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password update exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/password/delete")
    public ResponseEntity<?> delete(@RequestParam("id") String id) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(id)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = passwordService.delete(id);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password delete exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/password/categories")
    public ResponseEntity<?> categories() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<PasswordCategory> result = passwordService.categories(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password categories exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/password/category/add")
    public ResponseEntity<?> addCategory(@RequestBody PasswordRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getCategory())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            PasswordCategory result = passwordService.addCategory(loginUser.getUid(), request.getCategory(), request.getParentId());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password addCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/password/category/rename")
    public ResponseEntity<?> renameCategory(@RequestBody PasswordRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getId()) || !StringUtils.hasLength(request.getNewCategory())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = passwordService.renameCategory(loginUser.getUid(), request.getId(), request.getNewCategory());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password renameCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/password/category/delete")
    public ResponseEntity<?> deleteCategory(@RequestBody PasswordRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getId())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            String uid = loginUser.getUid();
            String id = request.getId();
            // 有子分类不能删除
            List<PasswordCategory> children = passwordService.queryCategoryByPid(uid, id);
            if (children.size() > 0) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EXISTED));
            }
            // 有关联账号不能删除
            List<PasswordEntry> entries = passwordService.queryEntryByCategoryId(uid, id);
            if (entries.size() > 0) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EXISTED));
            }
            boolean result = passwordService.deleteCategory(id);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("password deleteCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/password/category/move")
    public ResponseEntity<?> moveCategory(@RequestBody PasswordRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getId())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            String error = passwordService.moveCategory(loginUser.getUid(), request.getId(), request.getParentId());
            if (error != null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UPDATE_ERROR.getStatus(), error));
            }
            return ResponseEntity.ok(new ResultSuccess<>(true));
        } catch (Exception ex) {
            log.error("password moveCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/password/category/migrate")
    public ResponseEntity<?> migrateCategory() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            int migrated = passwordService.migrate();
            return ResponseEntity.ok(new ResultSuccess<>(migrated));
        } catch (Exception ex) {
            log.error("password migrateCategory exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
