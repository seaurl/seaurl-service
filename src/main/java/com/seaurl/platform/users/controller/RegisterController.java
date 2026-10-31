package com.seaurl.platform.users.controller;

import com.alibaba.fastjson.JSON;
import com.aliyun.tea.*;
import com.aliyun.captcha20230305.*;
import com.aliyun.captcha20230305.models.*;
import com.aliyun.teaopenapi.*;
import com.aliyun.teaopenapi.models.*;
import com.seaurl.platform.common.cache.TempCodeService;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.users.model.UserAuth;
import com.seaurl.platform.users.request.AccountRequest;
import com.seaurl.platform.users.request.RegisterRequest;
import com.seaurl.platform.users.request.UserPointsRequest;
import com.seaurl.platform.users.service.RegisterService;
import com.seaurl.platform.users.service.UserAuthService;
import com.seaurl.platform.users.service.UserService;
import com.seaurl.platform.users.service.email.SendRegisterEmailService;
import com.seaurl.platform.users.service.email.SendRegisterSuccessEmailService;
import com.seaurl.platform.users.service.aliyun.OssService;
import com.seaurl.platform.users.utils.AliyunVerify;
import com.seaurl.platform.users.utils.InputHelper;
import com.seaurl.common.result.ResultException;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.platform.users.service.AccountService;
import com.seaurl.platform.users.utils.IpUtils;
import io.micrometer.observation.annotation.Observed;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")

@Observed
public class RegisterController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    AccountService accountService;

    @Autowired
    UserAuthService userAuthService;

    @Autowired
    RegisterService registerService;

    @Autowired
    private TempCodeService tempCodeService;


    @Autowired
    private SendRegisterEmailService sendRegisterEmailService;

    @Autowired
    private SendRegisterSuccessEmailService sendRegisterSuccessEmailService;

    @Autowired
    private AliyunConfig aliyunConfig;

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
        log.info("---------ip={}", ip);
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
     * 用户注册
     *
     * @param registerRequest
     * @return
     */
    @PostMapping("/web/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest, ServerHttpRequest request) {

        // 1、用户名已存在或不正确 2、邮箱已存在或不正确 3、密码不正确
        String username = registerRequest.getUsername();
        String email = registerRequest.getEmail();
        String password = registerRequest.getPassword();
//        String clientIp = IpUtils.getIpAddr(request);
//
//        if (StringUtils.hasLength(clientIp)) {
//            // 将 IP 地址添加到 MDC
//            MDC.put("clientIp", clientIp);
//            log.info("----------------clientIp-------------{}", clientIp);
//        }
//        String captchaVerifyParam = registerRequest.getCaptchaVerifyParam();

        if (!StringUtils.hasLength(username) || !InputHelper.checkUsername(username)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USERNAME_WROND));
        }
        if (!StringUtils.hasLength(email) || !InputHelper.checkEmail(email)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_WROND));
        }
        if (!StringUtils.hasLength(password) || !InputHelper.checkPassword(password)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.PWD_WROND));
        }
        // 判断用户名或邮箱是否已存在
        if (StringUtils.hasLength(username) && registerService.hasUsername((username))) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USERNAME_EXIST));
        }
        if (StringUtils.hasLength(email) && registerService.hasEmail(email)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_EXIST));
        }
//        if (!StringUtils.hasLength(captchaVerifyParam)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }

        //校验
//        AliyunVerify aliyunVerify = new AliyunVerify();
//        boolean verifyResult = aliyunVerify.verify(aliyunConfig.getAccessKeyId(), aliyunConfig.getAccessKeySecret(), "130bzgw8", captchaVerifyParam);
//        log.info("verifyResult ={}", verifyResult);
//        if (!verifyResult){
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.VERIFIED_FAILURE));
//        }

        String uid = "";
        try {
            // 获取ip归属地
            String region = searchIpAddr(request);
            if (StringUtils.hasLength(region)) {
                // 设置Ip归属地
                registerRequest.setRegion(region);
            }

            //todo:用户注册之后把用户名推到redis中
            uid = registerService.register(registerRequest);
            if (StringUtils.hasLength(uid)) {
                // 注册成功添加1积分
                Map<String, String> userinfo = new HashMap<>();
                userinfo.put("username", username);
                userinfo.put("email", email);
                sendRegisterEmailService.sendRegisterEmail(username, email);

                return ResponseEntity.ok(new ResultInfo<>("注册成功，邮件已发送"));
            }
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.REGISTER_WROND));
        } catch (Exception e) {
            log.error("register exception ={}", e.getMessage());
            if (StringUtils.hasLength(uid)) {
                //如果失败要把注册的数据删除
                boolean result = accountService.deleteAccount(uid);
                log.info("注册失败，删除账户是否成功：{}", result);
            }
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.REGISTER_WROND));
        }
//        finally {
//            // 清除 MDC 中的 IP 地址
//            MDC.remove("clientIp");
//        }
    }

    /**
     * 重新发送注册验证邮箱
     * 使用username:resend:email保存到redis，时效一小时，防止用户重复提交
     *
     * @param username
     * @return
     */
    @PostMapping("/web/users/email/resend")
    public ResponseEntity<?> resend(@RequestBody RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
//        String captchaVerifyParam = registerRequest.getCaptchaVerifyParam();
//        log.info("resend username={},captchaVerifyParam={}", username, captchaVerifyParam);
        if (!StringUtils.hasLength(username)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        //校验
//        AliyunVerify aliyunVerify = new AliyunVerify();
//        boolean verifyResult = aliyunVerify.verify(aliyunConfig.getAccessKeyId(), aliyunConfig.getAccessKeySecret(), "130bzgw8", captchaVerifyParam);
//        log.info("verifyResult ={}", verifyResult);
//        if (!verifyResult){
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.VERIFIED_FAILURE));
//        }


        if (StringUtils.hasLength(username)) {
            UserAuth userAuth = userAuthService.getUserByName(username);
            if (userAuth == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String email = userAuth.getEmail();
            // 判断用户是否已验证
            if (userAuth.isVerified()) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.HAD_VERIFIED));
            }
            log.info("---username---{}", username + ":resend:" + email);
            String resendKey = username + ":resend:" + email;
            if (tempCodeService.hasKey(resendKey)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_HAD_SEND));
            }
            Map<String, String> userinfo = new HashMap<>();
            userinfo.put("username", username);
            userinfo.put("email", email);

            sendRegisterEmailService.sendRegisterEmail(username, email);

            try {
                tempCodeService.set(resendKey, email, 3600);
                log.info("---保存到 tempCode 成功---");
            } catch (Exception e) {
                log.error("---resend 保存 tempCode 失败---{}", e.getMessage());
            }
            return ResponseEntity.ok(new ResultInfo<>("邮件已发送，请注意查收"));
        } else {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USERNAME_WROND));
        }
    }

    /**
     * 验证用户邮箱
     *
     * @param accountRequest
     * @return
     */
    @PostMapping("/web/users/confirm/email/verified")
    public ResponseEntity<?> verified(@RequestBody AccountRequest accountRequest) {
        try {
//            log.info("开始验证邮箱：");
            String username = accountRequest.getUserName();
            String code = accountRequest.getCode();
            String email = "";
            if (!StringUtils.hasLength(username) || !StringUtils.hasLength(code)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            log.info("开始验证邮箱：username={},code={}", username, code);
            if (tempCodeService.hasKey(code)) {
                Map<String, String> cacheUserinfo = tempCodeService.hmget(code);
                email = cacheUserinfo.get("email");
                for (Map.Entry<String, String> vo : cacheUserinfo.entrySet()) {
                    log.info(vo.getKey() + " -- " + vo.getValue());
                }
                log.info("开始验证邮箱：email={}", email);
                //验证成功
                //修改用户状态
                boolean result = userAuthService.updateUserVerified(username);
                if (result) {
                    Map<String, String> userinfo = new HashMap<>();
                    userinfo.put("username", username);
                    userinfo.put("email", email);

                    sendRegisterSuccessEmailService.sendRegisterSuccessEmail(username, email);

                    log.info("验证邮箱成功");
                    tempCodeService.del(code);
                    tempCodeService.del(username + ":resend:" + email);
                    return ResponseEntity.ok(new ResultInfo<>("验证邮箱成功"));
                } else {
                    log.info("修改用户信息中是否验证字段isVerified失败");
                    return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_FAILURE));
                }
            } else {
                //code失效或者无效，请重新发送
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_CODE_ERROR));
            }
        } catch (Exception ex) {
            log.error("verified exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
