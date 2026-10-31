package com.seaurl.platform.users.service.email;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.platform.users.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SendRegisterSuccessEmailService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MailService mailService;

    /**
     * 发送注册成功欢迎邮件
     *
     * @param username 用户名
     * @param email    邮箱地址
     */
    public void sendRegisterSuccessEmail(String username, String email) {
        if (!StringUtils.hasLength(username) || !StringUtils.hasLength(email)) {
            log.info("用户名或邮箱为空，不能发送注册成功邮件");
            return;
        }

        log.info("发送注册成功邮件: username={}, email={}", username, email);
        ResultInfo resultInfo = mailService.sendRegisterSuccessEMail(username, email);
        if (resultInfo.getStatus() == 0) {
            log.info("发送注册成功邮件成功");
        } else {
            log.info("发送注册成功邮件失败: {}", resultInfo.getMessage());
        }
    }
}
