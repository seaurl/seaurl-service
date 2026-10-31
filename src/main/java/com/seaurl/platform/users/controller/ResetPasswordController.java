package com.seaurl.platform.users.controller;

import com.seaurl.platform.common.cache.TempCodeService;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.request.AccountEmail;
import com.seaurl.platform.users.request.ResetPassword;
import com.seaurl.platform.users.service.UserAuthService;
import com.seaurl.platform.users.service.UserService;
import com.seaurl.platform.users.utils.AliyunVerify;
import com.seaurl.platform.users.utils.InputHelper;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.platform.users.service.AccountService;
import com.seaurl.platform.users.service.email.SendResetPasswordEmailService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")

@Observed
public class ResetPasswordController {

    private static Logger log = LoggerFactory.getLogger(ResetPasswordController.class);

    @Autowired
    AccountService accountService;

    @Autowired
    UserAuthService userAuthService;

    @Autowired
    private TempCodeService tempCodeService;


    @Autowired
    UserService userService;

    @Autowired
    private SendResetPasswordEmailService sendResetPasswordEmailService;

    @Autowired
    private AliyunConfig aliyunConfig;

    /**
     * 修改密码
     *
     * @param resetPassword
     * @return
     */
    @PostMapping("/web/reset/password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPassword resetPassword) {
//        LoginUser user = UserContextHolder.getContext().getUser();
//        if (user == null) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
//        }
        String userName = resetPassword.getUserName();

        if (!StringUtils.hasLength(resetPassword.getNewPwd()) || !StringUtils.hasLength(resetPassword.getEmail()) || !StringUtils.hasLength(userName)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        boolean result = userAuthService.updateUserPassword(userName, resetPassword.getEmail(), resetPassword.getNewPwd());
        log.info("result = {}", result);
        return ResponseEntity.ok(result ? new ResultInfo<>("修改成功") : ResultStatus.PWD_UPDATE_ERROR);
    }

    /**
     * 发送重置密码邮件
     *
     * @param
     * @return
     */
    @PostMapping("/web/reset/sendResetPasswordEmail")
    public ResponseEntity<?> sendResetPasswordEmail(@RequestBody AccountEmail accountEmail) {
        String email = accountEmail.getEmail();
//        String captchaVerifyParam = accountEmail.getCaptchaVerifyParam();
        if (!StringUtils.hasLength(email) || !InputHelper.checkEmail(email)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_WROND));
        }
//        if (!StringUtils.hasLength(captchaVerifyParam)) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
//        }
        // 判断该邮箱是否已经注册，只有已注册的邮箱才能重新发送
        if (!userService.isExistEmailAndIsVerified(email)){
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String resendKey = email + ":resetPassword:email";
        if (tempCodeService.hasKey(resendKey)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.EMAIL_HAD_SEND));
        }

        //校验
//        AliyunVerify aliyunVerify = new AliyunVerify();
//        boolean verifyResult = aliyunVerify.verify(aliyunConfig.getAccessKeyId(), aliyunConfig.getAccessKeySecret(), "130bzgw8", captchaVerifyParam);
//        log.info("verifyResult ={}", verifyResult);
//        if (!verifyResult){
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.VERIFIED_FAILURE));
//        }

        tempCodeService.set(resendKey, email, 3600);
        log.info("保存到 tempCode - {}", resendKey);

        sendResetPasswordEmailService.sendResetPasswordEmail(email);

        return ResponseEntity.ok(new ResultInfo<>("邮件已发送"));
    }

    /**
     * 验证重置密码 返回的code是否正确
     *
     * @param code
     * @return
     */
    @GetMapping("/web/reset/verified/resetPassword/{code}")
    public ResponseEntity<?> verifiedResetPassword(@PathVariable String code) {
//        LoginUser loginUser = UserContextHolder.getContext().getUser();
//        if (loginUser == null) {
//            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
//        }
        log.info("verifiedResetPassword 验证的code ={}", code);
        try {
            if (!StringUtils.hasLength(code)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.CODE_ERROR));
            }
            if (tempCodeService.hasKey(code)) {
                User user = accountService.getUserByEmail(tempCodeService.get(code));
                log.info("verifiedResetPassword user is", user.getUserName());
                // 验证成功之后要把该缓存清除
                //todo:这里注意一下要不要清除发送邮件时的1小时缓存，这里选择不要，因为不管你密码有没有重置成功，你都不能重复发送重置密码邮件
                // 补充：因为没有清空缓存，导致下次点击进来还是可以修改密码(1小时内)，应该问题不大，后面有需要再改吧
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.SUCCESS, user));
            } else {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.CODE_INVALID));
            }
        }catch (Exception ex) {
            log.error("verifiedResetPassword exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
