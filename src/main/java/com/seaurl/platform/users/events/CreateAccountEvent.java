package com.seaurl.platform.users.events;

import com.alibaba.fastjson.JSONObject;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.platform.users.service.MailService;
import com.seaurl.platform.users.service.MailVerifiedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CreateAccountEvent {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    MailService mailService;

    @Autowired
    MailVerifiedService mailVerifiedService;

    /**
     * 发送重置密码邮件
     *
     * @param email
     */
//    @RabbitListener(queuesToDeclare = @Queue("resetPassword.sendEmail.queue"))
//    public void sendResetPasswordEmailMessage(String email) {
//        if (StringUtils.hasLength(email)) {
//            String code = UUID.randomUUID().toString().replaceAll("-", "");
//            //发送验证邮箱
//            ResultInfo resultInfo = mailService.sendResetPasswordEMail(email, code);
//            log.info(resultInfo.getMessage());
//            if (resultInfo.getStatus() == 0) {
//                // 限制一小时内验证
//                try {
//                    // 这个code后面验证的时候要
//                    globalCache.set(code, email,3600);
//                    log.info("---保存redis成功---");
//                } catch (Exception e) {
//                    log.info("---保存redis失败---{}", e.getMessage());
//                }
//            } else {
//                log.info("---发送邮件失败---{}", resultInfo.getMessage());
//            }
//        }else{
//            log.info("email不正确");
//        }
//    }

    /**
     * 发送注册邮箱信息
     */
//    @RabbitListener(queuesToDeclare = @Queue("register.sendEmail.queue"))
//    public void sendRegisterEmailMessage(Map<String, Object> userinfo) {
//        String username = "";
//        String email = "";
//        for (Map.Entry<String, Object> vo : userinfo.entrySet()) {
//            if ("username".equals(vo.getKey())) {
//                username = (String) vo.getValue();
//            }
//            if ("email".equals(vo.getKey())) {
//                email = (String) vo.getValue();
//            }
//            log.info(vo.getKey() + " -=- " + vo.getValue());
//        }
//        String code = UUID.randomUUID().toString().replaceAll("-", "");
//        if (StringUtils.hasLength(username) && StringUtils.hasLength(email)) {
//            //发送验证邮箱
//            ResultInfo resultInfo = mailService.sendVerifiedMail(username, email, code);
//            log.info(resultInfo.getMessage());
//            if (resultInfo.getStatus() == 0) {
//                // 限制一小时内验证
//                try {
//                    // 作用：向redis中保存发送更新验证邮箱记录,供用户验证使用
//                    // 格式：code:userinfo
//                    // 时效：1小时
//                    globalCache.hmset(code, userinfo, 3600);
//                    log.info("---保存redis成功---");
//                } catch (Exception e) {
//                    log.info("---保存redis失败---{}", e.getMessage());
//                }
//            } else {
//                log.info("---发送邮件失败---{}", resultInfo.getMessage());
//            }
//        }
//    }

    /**
     * 发送注册成功邮件
     *
     * @param userinfo
     */
//    @RabbitListener(queuesToDeclare = @Queue("registerSuccess.sendEmail.queue"))
//    public void sendRegisterSuccessEmailMessage(Map<String, String> userinfo) {
//        String username = "";
//        String email = "";
//        for (Map.Entry<String, String> vo : userinfo.entrySet()) {
//            if ("username".equals(vo.getKey())) {
//                username = vo.getValue();
//            }
//            if ("email".equals(vo.getKey())) {
//                email = vo.getValue();
//            }
//            log.info(vo.getKey() + " -=- " + vo.getValue());
//        }
//        if (StringUtils.hasLength(username) && StringUtils.hasLength(email)) {
//            //发送验证邮箱
//            ResultInfo resultInfo = mailService.sendRegisterSuccessEMail(username, email);
//            if (resultInfo.getStatus() == 0) {
//                log.info("发送注册成功邮件成功");
//            } else {
//                log.info("发送注册成功邮件失败" + resultInfo.getMessage());
//            }
//        }else{
//            log.info("邮箱或用户名为空，不能发送，终止！");
//        }
//    }


    /**
     * 发送更新邮件消息
     */
//    @RabbitListener(queuesToDeclare = @Queue("updateEmail.sendEmail.queue"))
//    public void sendUpdateEmailMessage(Map<String, Object> userinfo){
//        String username = "";
//        String email = "";
//        for (Map.Entry<String, Object> vo : userinfo.entrySet()) {
//            if ("username".equals(vo.getKey())) {
//                username = (String) vo.getValue();
//            }
//            if ("email".equals(vo.getKey())) {
//                email = (String) vo.getValue();
//            }
//            log.info(vo.getKey() + " -=- " + vo.getValue());
//        }
//
//        if (StringUtils.hasLength(username) && StringUtils.hasLength(email)) {
//            String code = UUID.randomUUID().toString().replaceAll("-", "");
//
//            //发送验证邮箱
//            ResultInfo resultInfo = mailService.sendUpdateMail(username, email, code);
//            log.info(resultInfo.getMessage());
//            if (resultInfo.getStatus() == 0) {
//                // 限制一小时内验证
//                try {
//                    // 作用：向redis中保存发送更新验证邮箱记录,供用户验证使用
//                    // 格式：code:userinfo
//                    // 时效：1小时
//                    globalCache.hmset(code, userinfo, 3600);
//                    log.info("---保存redis成功---");
//                } catch (Exception e) {
//                    log.info("---保存redis失败---{}", e.getMessage());
//                }
//            } else {
//                log.info("---发送邮件失败---{}", resultInfo.getMessage());
//            }
//        }else{
//            log.info("邮箱或用户名为空，不能发送，终止！");
//        }
//    }



//    @Value("${server.port}")
//    String port;

//    @GetMapping("/hi")
//    public String hi(@RequestParam(value = "name", defaultValue = "forezp", required = false) String name) {
//        return "hello " + name + ", i'm provider ,my port:" + port;
//
//    }
//
//    //
//    @RabbitListener(queuesToDeclare = @Queue("test.queue"))
//    public void handle1(String msg) {
//        log.info("方法1已接收到消息：{}", msg);
//    }

}
