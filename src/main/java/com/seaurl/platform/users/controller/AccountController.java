package com.seaurl.platform.users.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.seaurl.platform.common.auth.UserSessionService;
import com.seaurl.platform.common.cache.TempCodeService;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.common.config.ServiceConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.common.model.UserSession;
import com.seaurl.platform.users.model.UserAuth;
import com.seaurl.platform.users.model.UserDevice;
import com.seaurl.platform.users.request.*;
import com.seaurl.platform.users.service.*;
import com.seaurl.platform.users.service.email.SendUpdateEmailService;
import com.seaurl.platform.users.utils.AliyunVerify;
import com.seaurl.platform.users.utils.InputHelper;
import com.seaurl.platform.users.utils.IpUtils;
import com.seaurl.platform.users.utils.JwtTokenUtil;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import io.micrometer.observation.annotation.Observed;
import org.apache.commons.codec.binary.Base64;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/users")

@Observed
public class AccountController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    AccountService accountService;

    @Autowired
    UserAuthService userAuthService;

    @Autowired
    RegisterService registerService;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

//    @Autowired
//    AuthFeignClient authFeignClient;

    @Autowired
    ServiceConfig serviceConfig;

    @Autowired
    private AliyunConfig aliyunConfig;

    @Autowired
    private UserDeviceService userDeviceService;

    @Autowired
    private SendUpdateEmailService sendUpdateEmailService;

    // 30天，单位秒
    private static final int COOKIE_EXPIRE_TIME = 30 * 24 * 60 * 60;
    // 15天，单位秒
    private static final int COOKIE_EXPIRE_LT_TIME = 15 * 24 * 60 * 60;
    // 6天
//    private static final int REDIS_EXPIRE_TIME = 6 * 24 * 60 * 60;

    private static final String SESSION_KEY = "session_jti";
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private TempCodeService tempCodeService;


    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    public String searchIpAddr(ServerHttpRequest request) {
        String ip = IpUtils.getIpAddr(request);
        logger.info("---------ip={}", ip);
        Searcher searcher = null;
        try {
            InputStream fileStream = new ClassPathResource("ipdb/ip2region.xdb").getInputStream();
            searcher = Searcher.newWithBuffer(toByteArray(fileStream));
        } catch (IOException e) {
            return "";
        }
        try {
            String region = searcher.search(ip);
            searcher.close();
            return region;

        } catch (Exception e) {
        }
        return "";
    }

    /**
     * 更新用户ip归属地
     *
     * @param request
     * @param uid
     */
    private void updateUserRegion(ServerHttpRequest request, String uid) {
        // 把ip归属地也加入到用户信息中
        String region = searchIpAddr(request);
        if (StringUtils.hasLength(region) && StringUtils.hasLength(uid)) {
            logger.info("uid={},region={}", uid, region);
            accountService.updateUserIpAddr(uid, region);
        }
    }

    //获取ip地址
//    public ResponseEntity<?> getIpAddr(ServerHttpRequest request){
//        String ip = IpUtils.getIpAddr(request);
//        return ResponseEntity.ok(new ResultSuccess<>(ip));
//    }


//    public ResponseEntity<?> handleBlock(AuthRequest request, BlockException ex) {
//        // Handle blocked requests
//        return ResponseEntity.ok(new ResultInfo<>(ResultStatus.SERVER_Exception));
//    }
//
//    public ResponseEntity<?> handleFallback(AuthRequest request) {
//        // Handle fallback cases (e.g., timeout, etc.)
//        return ResponseEntity.ok(new ResultInfo<>(ResultStatus.SERVER_Exception));
//    }

    /**
     * 退出所有设备登录
     *
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/web/logoutAll")
    public ResponseEntity<?> logoutAll(ServerHttpRequest request, ServerHttpResponse response) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            // 说明已经退出
            return ResponseEntity.ok(new ResultSuccess<>(true));
        }
        logger.info("开始退出登录了，用户是：{}", loginUser.getUserName());
        try {
            List<UserDevice> userDeviceList = userDeviceService.queryByUid(loginUser.getUid());
            userDeviceList.forEach(userDevice -> {
                String tokenId = userDevice.getTokenId();
                // 设置token过期，保证就算黑客获取了tokenid也登录不了
                userSessionService.delete(tokenId);
            });
            logger.info("1、退出并设置redis过期了");
            boolean result = userDeviceService.deleteAll(loginUser.getUid());
            logger.info("2、删除表中所有该用户的所有设备信息结果：{}", result);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception exception) {
            logger.error("logoutAll exception={}", exception.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Unknown_Exception));
        }
    }

    /**
     * 仅退出当前设备
     *
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/web/logout")
    public ResponseEntity<?> logoutCurrent(ServerHttpRequest request, ServerHttpResponse response) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            // 说明已经退出
            return ResponseEntity.ok(new ResultSuccess<>(true));
        }
        String tokenId = request.getHeaders().getFirst("token-id");
        if (!StringUtils.hasLength(tokenId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        logger.info("开始退出登录了，用户是：{}", loginUser.getUserName());
        try {
            userDeviceService.deleteById(tokenId);
            userSessionService.delete(tokenId);
            logger.info("1、退出并设置redis过期了");
            boolean result = userDeviceService.deleteAll(loginUser.getUid());
            logger.info("2、删除表中该用户的当前设备信息结果：{}", result);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception exception) {
            logger.error("logoutCurrent exception={}", exception.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Unknown_Exception));
        }
    }

    /**
     * 登录
     * 1、根据用户名和密码生成token
     * 2、将token保存到redis中
     * 3、将jti放到cookie中
     *
     * @param authRequest
     * @return
     */
    @PostMapping("/web/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest, ServerHttpRequest request, ServerHttpResponse response) throws UnknownHostException {
//        logger.info("进来了");

        // 注意：用户名可以是邮箱、手机号、用户名
        String userName = authRequest.getUserName();
        String password = authRequest.getPassword();


        if (!StringUtils.hasLength(userName) || !StringUtils.hasLength(password)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {

//            logger.info("ipAddress={}", ip);
//            if ("127.0.0.1".equals(ip) || ip.startsWith("192.168")) {
//                logger.info("查看到的是本地或者局域网Ip");
//            }else{
//                String region = searchIpAddr(ip);
//                logger.info("region={}", region);
//            }

            // 根据用户名查询用户认证信息
            UserAuth userAuth = userAuthService.getUserByName(userName);
            if (userAuth == null) {
                logger.info("login userAuth is null for: {}", userName);
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USERNAME_WROND));
            }
            if (!userAuth.isVerified()) {
                logger.info("login user not verified: {}", userName);
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.VERIFIED_FAILURE));
            }
            // 根据用户id查询用户详情
            User user = accountService.getUserById(userAuth.getUid());
            if (user == null) {
                logger.info("login user detail not found for uid: {}", userAuth.getUid());
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USERNAME_WROND));
            }

            // 比较两次密码是否相同
            String storedPwd = userAuth.getPassword();
            logger.info("login pwd check: storedPwd exists={}, pwdLen={}", storedPwd != null, storedPwd != null ? storedPwd.length() : 0);
            boolean isAuth = bCryptPasswordEncoder.matches(password, storedPwd);

            if (!isAuth) {
                logger.info("获取token失败, password={}, storedPwdPrefix={}", password, storedPwd != null ? storedPwd.substring(0, Math.min(20, storedPwd.length())) : "null");
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_AUTH_ERROR));
            }

            // 把ip归属地也加入到用户信息中
            updateUserRegion(request, userAuth.getUid());

            UUID jti = UUID.randomUUID();
            Map<String, Object> claims = new HashMap<>();
            claims.put("userName", userAuth.getUserName());
            claims.put("roles", userAuth.getRoles());
            claims.put("email", userAuth.getEmail());
            claims.put("uid", userAuth.getUid());
            claims.put("avatar", user.getAvatar());
            claims.put("background", user.getBackground());
            claims.put("color", user.getColor());
            logger.info("motile={}", user.getMobile());

            if (user.getMobile() == null) {
                claims.put("mobile", "");
            } else {
                claims.put("mobile", user.getMobile());
            }
//
            claims.put("jti", jti);

            String token = jwtTokenUtil.generateToken(claims);
            logger.info("获取到token={}", token);

            if (StringUtils.hasLength(token)) {
                // 用户在10个设备登录就有10个tokenID，如果某个设备退出登录，其它设备也跟着退出呢？
                UserSession userSession = userSessionService.create(user.getUid(), token, COOKIE_EXPIRE_TIME);
                String tokenId = userSession.getId();
                logger.info("COOKIE_EXPIRE_TIME={}", COOKIE_EXPIRE_TIME);

                // 添加一条设备信息
                UserDevice userDevice = new UserDevice();
                Date date = new Date();
                userDevice.setUid(user.getUid());
                userDevice.setCreatedDt(date);
                userDevice.setUpdatedDt(date);
                userDevice.setTokenId(tokenId);
                userDeviceService.add(userDevice);

                Map<String, Object> userInfo = new HashMap<>();
//            userInfo.put("id", jti);
                userInfo.put("uid", user.getUid());
                userInfo.put("tokenId", tokenId);
                userInfo.put("name", userAuth.getUserName());
                userInfo.put("image", user.getAvatar());
                userInfo.put("email", user.getEmail());
                userInfo.put("background", user.getBackground());
                userInfo.put("color", user.getColor());
                return ResponseEntity.ok(new ResultSuccess<>(userInfo));
            }

            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_AUTH_ERROR));
        } catch (Exception ex) {
            logger.error("login exception ={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_AUTH_ERROR));
        }
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/crud/refreshToken")
    public ResponseEntity<?> refreshToken(ServerHttpRequest request, ServerHttpResponse response) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();

            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.AUTH_ERROR));
            }

            // 查询是否包含此token的设备，如果没有则返回验证失败，交给前端处理
            List<UserDevice> userDeviceList = userDeviceService.queryByUid(loginUser.getUid());
            if (userDeviceList.isEmpty()) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.AUTH_ERROR));
            }
            logger.info("当前登录的用户名是：{}", loginUser.getUserName());
            logger.info("---------------refreshToken------------------");
            String tokenId = request.getHeaders().getFirst("token-id");
            logger.info("token id ={}", tokenId);
            long sessionExpire = userSessionService.remainingSeconds(tokenId);
            if (StringUtils.hasLength(tokenId) && sessionExpire >= 0) {
                // 默认是老的，除非过期创建新的
                String newTokenId = tokenId;
                logger.info("newTokenId id1 ={}", tokenId);

                //  注意：如果过期只能删除当前设备的相关信息和退出场景不一样
                if (sessionExpire > 1 && sessionExpire < COOKIE_EXPIRE_LT_TIME) {
                    logger.info("token快要过期了，我来续期一下");
                    newTokenId = userSessionService.renew(tokenId, COOKIE_EXPIRE_TIME);
                    logger.info("newTokenId id2 ={}", newTokenId);

                    if (newTokenId != null) {
                        // 防止黑客拿到过期的也能登录
                        userDeviceService.deleteById(tokenId);

                        Date date = new Date();
                        UserDevice userDevice = new UserDevice();

                        userDevice.setTokenId(newTokenId);
                        userDevice.setUid(loginUser.getUid());
                        userDevice.setCreatedDt(date);
                        userDevice.setUpdatedDt(date);

                        userDeviceService.add(userDevice);
                    } else {
                        return ResponseEntity.ok(new ResultInfo<>(ResultStatus.AUTH_ERROR));
                    }
                } else {
                    logger.info("token没有过期");
                }
                // 如果有登录用户，则更新登录人的ip归属地
                // 把ip地址也加入到用户信息中
                updateUserRegion(request, loginUser.getUid());
                logger.info("newTokenId id3 ={}", newTokenId);
                return ResponseEntity.ok(new ResultSuccess<>(newTokenId));
            }
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.AUTH_ERROR));
        } catch (Exception ex) {
            logger.error("refreshToken exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Unknown_Exception));
        }
    }

    private HashMap getAuthenticationInfo(String authToken) {
//        JSONObject authinfo = decodeJWT(authToken);
        String[] split_string = authToken.split("\\.");
        String base64EncodedBody = split_string[1];
        org.apache.commons.codec.binary.Base64 base64Url = new Base64(true);
        String body = new String(base64Url.decode(base64EncodedBody));
        HashMap<String, String> map = JSON.parseObject(body, new TypeReference<HashMap<String, String>>() {
        });

//        String uid = authinfo.getString("uid");
//        String userName = authinfo.getString("userName");
//        String email = authinfo.getString("email");
//        String avatar = authinfo.getString("avatar");
//        String mobile = authinfo.getString("mobile");
////        logger.info("开始解析jwt token");
////        logger.info("jwt token uid={}", uid);
////        logger.info("cookid ={}.解析结果：userName={}", cookid, userName);
//        HashMap user = new HashMap();
//        user.put("uid", uid);
//        user.put("userName", userName);
//        user.put("email", email);
//        user.put("avatar", avatar);
//        user.put("mobile", mobile);

        // {"user_name":"zhangwei","scope":["all"],"authorities":["ROLE_USER"],"jti":"799ac2d0-0662-4fcd-a567-fa93356362a0","client_id":"test-seaurl"}
        return map;
    }


    /**
     * 续期cookie过程
     * 1、cookie key重新生成，并设置到浏览器
     * 2、老的删除，创建新的redis key=xxx并保存token，时间和cookie时间相同
     * 注意：浏览器只发送key-name的cookie到后台，而发送不了对应的过期时间，我也不知道为什么！
     *
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/crud/refresh")
    public ResponseEntity<?> refresh(ServerHttpRequest request, ServerHttpResponse response) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            logger.info("---------------refresh------------------");
            Map<String, List<HttpCookie>> cookies = request.getCookies();
            List<HttpCookie> myCookies = cookies.get(SESSION_KEY);
            if (myCookies != null && !myCookies.isEmpty()) {
                String oldCookieKey = myCookies.get(0).getValue();

                if (StringUtils.hasLength(oldCookieKey)) {
                    long sessionExpire = userSessionService.remainingSeconds(oldCookieKey);
                    if (sessionExpire > 1 && sessionExpire < COOKIE_EXPIRE_LT_TIME) {
                        logger.info("cookie快要过期了，我来续期一下");
                        String renewed = userSessionService.renew(oldCookieKey, COOKIE_EXPIRE_TIME);
                        if (renewed != null) {
                            //cookie未过期，继续使用
                            expireCookie(renewed, COOKIE_EXPIRE_TIME, response);
                        }
                    } else {
                        logger.info("cookie没有过期");
                    }
                    // 如果有登录用户，则更新登录人的ip归属地
                    if (loginUser != null) {
                        logger.info("---------------refresh------------------{}", loginUser.getUserName());
                        // 把ip地址也加入到用户信息中
                        updateUserRegion(request, loginUser.getUid());
                    }
                    return ResponseEntity.ok(new ResultSuccess<>(true));
                }
            }
            return ResponseEntity.ok(new ResultSuccess<>(ResultStatus.AUTH_ERROR));
        } catch (Exception ex) {
            logger.error("getMailbox exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    // 延期cookie
    private void expireCookie(String cookieValue, Integer cookieTime, ServerHttpResponse response) {
        ResponseCookie cookie = ResponseCookie.from(SESSION_KEY, cookieValue) // key & value
                .httpOnly(true)        // 禁止js读取
                .secure(true)        // 在http下也传输
                .domain(serviceConfig.getDomain())// 域名
                .path("/")            // path，过期用秒，不过期用天
                .maxAge(Duration.ofSeconds(cookieTime))
                .sameSite("Lax")    // 大多数情况也是不发送第三方 Cookie，但是导航到目标网址的 Get 请求除外
                .build();
//        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.getHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
    }


    /**
     * 退出登录
     *
     * @param request
     * @param response
     */
//    @PostMapping("/web/logout")
//    public void logout(ServerHttpRequest request, ServerHttpResponse response) {
//        try {
//            Map<String, List<HttpCookie>> cookies = request.getCookies();
//            List<HttpCookie> myCookies = cookies.get("session_jti");
//            if (myCookies != null && !myCookies.isEmpty()) {
//                String myCookieValue = myCookies.get(0).getValue();
//                if (StringUtils.hasLength(myCookieValue)) {
//                    globalCache.del(myCookieValue);
//                    ResponseCookie clearCookie = ResponseCookie.from("session_jti", "") // key & value
//                            .httpOnly(true)        // 禁止js读取
//                            .secure(true)        // 在http下也传输
//                            .domain(serviceConfig.getDomain())// 域名
//                            .path("/")            // path
//                            .maxAge(0)    // 1个小时候过期
//                            .sameSite("None")    // 大多数情况也是不发送第三方 Cookie，但是导航到目标网址的 Get 请求除外
//                            .build();
//                    response.getHeaders().add(HttpHeaders.SET_COOKIE, clearCookie.toString());
//                }
//            } else {
//                logger.info("cant found cookie is session_jti");
//            }
//        } catch (Exception ex) {
//            logger.error("refreshToken exception={}", ex.getMessage());
//        }
//    }

    /**
     * 获取用户基本信息
     *
     * @return
     */
    @GetMapping("/crud/me")
    public ResponseEntity<?> user() {
        try {
            logger.info("------- convert user={} -------", UserContextHolder.getContext().getUser());
            LoginUser user = UserContextHolder.getContext().getUser();
            if (user == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            } else {
                User latestUserInfo = accountService.getUserById(user.getUid());
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("name", latestUserInfo.getUserName());
                userInfo.put("avatar", latestUserInfo.getAvatar());
                userInfo.put("email", latestUserInfo.getEmail());
                userInfo.put("nickName", latestUserInfo.getNickName());
                return ResponseEntity.ok(userInfo);
            }
        } catch (Exception ex) {
            logger.error("user exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 获取用户基本信息
     *
     * @param userName
     * @return
     */
    @GetMapping("/userInfo/{userName}")
    public Mono<ResponseEntity<?>> getAccountInfoByUserName(@PathVariable String userName) {
        try {
            logger.info("in here? account={}", userName);
            if (!StringUtils.hasLength(userName)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }
            Mono<User> user = accountService.getUserByNameMono(userName);
            //todo:如果user返回没有值，怎么办？
            return Mono.just(ResponseEntity.ok(user));
        } catch (Exception ex) {
            logger.error("getAccountInfoByUserName exception={}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * @param uid
     * @return
     */
    @GetMapping("/info/getUserInfoByUid/{uid}")
    public ResponseEntity<?> getAccountInfoByUseId(@PathVariable String uid) {
        try {
            if (!StringUtils.hasLength(uid)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            User user = accountService.getUserById(uid);
            return ResponseEntity.ok(Objects.requireNonNullElseGet(user, () -> new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));

        } catch (Exception ex) {
            logger.error("getAccountInfoByUseId exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 更新用户基本信息
     *
     * @param userRequest
     * @return
     */
    @PostMapping("/crud/updateAccountBaseInfo")
    public ResponseEntity<?> updateAccountBaseInfo(@RequestBody UserRequest userRequest) {
        try {
            LoginUser user = UserContextHolder.getContext().getUser();
            if (user == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            userRequest.setUserName(user.getUserName());
            try {
                accountService.updateUserInfo(userRequest);
                return ResponseEntity.ok(new ResultInfo<>("更新成功"));
            } catch (Exception e) {
                logger.info("updateAccountBaseInfo Error=>" + e.getMessage());
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UPDATE_ERROR));
            }
        } catch (Exception ex) {
            logger.error("updateAccountBaseInfo exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据用户名查询用户基本信息
     *
     * @return
     */
    @GetMapping("/crud/getAccountBaseInfo")
    public ResponseEntity<?> getAccountBaseInfo() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            logger.info("getAccountBaseInfo username={}", loginUser.getUserName());
            User user = accountService.getUserByName(loginUser.getUserName());
            return ResponseEntity.ok(new ResultSuccess(user));

        } catch (Exception e) {
            logger.error("updateAccountBaseInfo Error=>" + e.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UPDATE_ERROR));
        }
    }


    /**
     * 上传用户头像
     * 注意：toBlob是有损上传，toDataURL(base64)是无损上传
     *
     * @param uploadAvatarRequest
     * @return
     */
    @PostMapping("/crud/uploadAvatar")
    public ResponseEntity<?> uploadAvatar(@RequestBody UploadAvatarRequest uploadAvatarRequest) {
        logger.info("uploadAvatar userName=>{}", uploadAvatarRequest.getUserName());
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        String avatarBase64 = uploadAvatarRequest.getAvatar();
        try {
            if (!StringUtils.hasLength(avatarBase64)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
//            String folder = "users/avatar/" + loginUser.getUserName() + ".png";
            String env = serviceConfig.getServerEnv();
            String avatar = "users/avatar/" + loginUser.getUserName() + "/" + env + "/" + System.currentTimeMillis();
            String fullpath = accountService.saveAvatar(avatarBase64, avatar);
            accountService.updateUserAvatar(loginUser.getUserName(), fullpath);

            return ResponseEntity.ok(new ResultSuccess<>(fullpath));
        } catch (Exception e) {
            logger.error("uploadAvatar Error=>{}", e.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UPLOAD_ERROR));
        }
    }

    /**
     * 修改账户名
     *
     * @param accountRequest
     * @return
     */
    @PostMapping("/crud/updateAccountUserName")
    public ResponseEntity<?> updateAccountUserName(@RequestBody AccountRequest accountRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String newUserName = accountRequest.getNewUserName();
            if (!StringUtils.hasLength(newUserName) || !InputHelper.checkUsername(newUserName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USERNAME_WROND));
            }

            // 判断是否有重名，是否符合命名规则
            if (registerService.hasUsername((newUserName))) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USERNAME_EXIST));
            }

            userAuthService.updateUserName(loginUser.getUserName(), newUserName);
            accountService.updateUserName(loginUser.getUserName(), newUserName);

            return ResponseEntity.ok(new ResultSuccess<>("修改成功"));
        } catch (Exception ex) {
            logger.error("updateAccountUserName exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 修改外观
     *
     * @param accountRequest
     * @return
     */
    @PostMapping("/crud/updateAccountAppearance")
    public ResponseEntity<?> updateAccountAppearance(@RequestBody AccountRequest accountRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String background = accountRequest.getBackground();
            String color = accountRequest.getColor();
//            if (!StringUtils.hasLength(color) || !StringUtils.hasLength(background)) {
//                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USERNAME_WROND));
//            }
            boolean result = accountService.updateAppearance(loginUser.getUid(), color, background);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            logger.error("updateAccountUserName exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 修改用户密码
     *
     * @param accountRequest
     * @return
     */
    @PostMapping("/crud/updateAccountPassword")
    public ResponseEntity<?> updateAccountPassword(@RequestBody AccountRequest accountRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String oldPwd = accountRequest.getOldPwd();
            String newPwd = accountRequest.getNewPwd();

            // 是否为空
            if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            UserAuth userAuth = userAuthService.getUserByName(loginUser.getUserName());
            // 原密码不正确
            if (!bCryptPasswordEncoder.matches(oldPwd, userAuth.getPassword())) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.OLD_PWD_ERROR));
            }
            Boolean result = userAuthService.updateUserPassword(loginUser.getUserName(), bCryptPasswordEncoder.encode(newPwd));
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            logger.error("updateAccountPassword exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 修改通知与提醒
     *
     * @param accountNotice
     * @return
     */
    @PostMapping("/crud/updateAccountNotice")
    public ResponseEntity<?> updateAccountNotice(@RequestBody AccountNotice accountNotice) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            accountNotice.setUserName(loginUser.getUserName());
            Boolean result = accountService.updateUserNotice(accountNotice);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            logger.error("updateAccountNotice exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 发送更新账户邮箱邮件
     *
     * @param accountEmail
     * @return
     */
    @PostMapping("/crud/sendUpdateAccountEmail")
    public ResponseEntity<?> sendUpdateAccountEmail(@RequestBody AccountEmail accountEmail) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String newEmail = accountEmail.getNewEmail();
//        String captchaVerifyParam = accountEmail.getCaptchaVerifyParam();

        if (!StringUtils.hasLength(newEmail)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        boolean existEmail = accountService.isExistEmail(newEmail);
        // 判断新邮箱是不是被人用过了
        // 用过则不能发送
        if (existEmail) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_EXIST));
        }
        //校验
//        AliyunVerify aliyunVerify = new AliyunVerify();
//        boolean verifyResult = aliyunVerify.verify(aliyunConfig.getAccessKeyId(), aliyunConfig.getAccessKeySecret(), "130bzgw8", captchaVerifyParam);
//        logger.info("verifyResult ={}", verifyResult);
//        if (!verifyResult) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.VERIFIED_FAILURE));
//        }

        String updateKey = loginUser.getUserName() + ":update:" + newEmail;
        logger.info("updateKey = {}", updateKey);
        if (tempCodeService.hasKey(updateKey)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_HAD_SEND));
        }
        Map<String, String> userinfo = new HashMap<>();
        userinfo.put("username", loginUser.getUserName());
        userinfo.put("email", newEmail);


        String result = JSON.toJSONString(userinfo);

        sendUpdateEmailService.sendUpdateEmail(loginUser.getUserName(), newEmail);

        try {
            tempCodeService.set(updateKey, newEmail, 3600);
            logger.info("---保存到 tempCode 成功---");
        } catch (Exception e) {
            logger.error("---保存 tempCode 失败---{}", e.getMessage());
        }
        return ResponseEntity.ok(new ResultInfo<>("邮件已发送，请注意查收"));
    }

    /**
     * 验证更新邮箱的邮件
     *
     * @param accountRequest
     * @return
     */
    @PostMapping("/info/update/email/verified")
    public ResponseEntity<?> verified(@RequestBody AccountRequest accountRequest) {
        try {
            String userName = accountRequest.getUserName();
            if (!StringUtils.hasLength(userName)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String code = accountRequest.getCode();
            logger.info("username={},code={}", userName, code);
            String email = "";
            if (!StringUtils.hasLength(code)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            if (tempCodeService.hasKey(code)) {
                Map<String, String> cacheUserinfo = tempCodeService.hmget(code);
                email = cacheUserinfo.get("email");
                for (Map.Entry<String, String> vo : cacheUserinfo.entrySet()) {
                    logger.info(vo.getKey() + " -- " + vo.getValue());
                }
                // updateUserEmail功能：更新User表邮箱，更新UserAuth表邮箱
                boolean result = userAuthService.updateUserEmail(userName, email);
                if (result) {
                    logger.info("验证成功，修改邮箱");
                    tempCodeService.del(code);
                    //todo:
                    //验证完之后把防止重复提交的缓存也清空
                    tempCodeService.del(userName + ":update:" + email);
                    return ResponseEntity.ok(new ResultInfo<>("验证邮箱成功"));
                } else {
                    return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_FAILURE));
                }
            } else {
                //code失效或者无效，请重新发送
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_CODE_ERROR));
            }
        } catch (Exception ex) {
            logger.error("verified exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
