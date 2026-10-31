package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.model.NoteCategory;
import com.seaurl.platform.common.model.UrlCategory;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.service.NoteCategoryService;
import com.seaurl.platform.space.service.UrlCategoryService;
import com.seaurl.platform.space.service.UserService;
import com.seaurl.platform.space.service.facade.UsersFacade;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/space")

@Observed
public class UserSpaceController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    UserService userService;

    @Autowired
    UrlCategoryService urlCategoryService;

    @Autowired
    NoteCategoryService noteCategoryService;

    @Autowired
    UsersFacade usersFacade;



    /**
     * 根据url中用户名获取用户信息
     *
     * @param userName
     * @return
     */
//    @GetMapping("/info/user/{userName}")
//    public Mono<ResponseEntity<?>> getUserInfoAndUrlByUserName(@PathVariable String userName) {
//        try {
//            if (!StringUtils.hasLength(userName)) {
//                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
//            }
//            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
//            return userMono.map(user -> {
//                Map<String, Object> userinfo = new HashMap<>();
//                userinfo.put("userInfo", user);
////        userinfo.put("urlCategories", urlCategoryList);
//                return ResponseEntity.ok(new ResultSuccess<>(userinfo));
//            });
//        }catch (Exception ex) {
//            log.error("getUserInfoAndUrlByUserName exception={}", ex.getMessage());
//            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
//        }
//    }
//
//    /**
//     *
//     * @param userName
//     * @return
//     */
//    @GetMapping("/info/user/note/{userName}")
//    public ResponseEntity<?> getUserInfoAndNoteByUserName(@PathVariable String userName) {
//        if (!StringUtils.hasLength(userName)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
//        User user = accountFeignClient.getAccountInfoByUserName(userName);
//        List<NoteCategory> urlCategoryList = noteCategoryService.queryCategoryByUid(user.getUid());
//        Map<String, Object> userinfo = new HashMap<>();
//        userinfo.put("userInfo", user);
//        userinfo.put("noteCategories", urlCategoryList);
//        return ResponseEntity.ok(new ResultSuccess<>(userinfo));
//    }

    /**
     * 获取空间统计数据
     * @param userName
     * @return
     */
    @GetMapping("/info/{userName}/statisticalData")
    public Mono<ResponseEntity<?>> getStatisticalData(@PathVariable String userName) {
        try {
            if (!StringUtils.hasLength(userName)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }
            Mono<User> userMono = usersFacade.getAccountInfoByUserName(userName);
            return userMono.map(user -> {
                Map<String, Object> dataMap = userService.getStatisticalData(user.getUid());
                return ResponseEntity.ok(new ResultSuccess<>(dataMap));
            });
        }catch (Exception ex) {
            log.error("getStatisticalData exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }
}
