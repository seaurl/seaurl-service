package com.seaurl.platform.users.service.email;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.platform.common.cache.TempCodeService;
import com.seaurl.platform.users.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SendRegisterEmailService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MailService mailService;

    @Autowired
    private TempCodeService tempCodeService;

    /**
     * 发送注册验证邮件
     *
     * @param username 用户名
     * @param email    邮箱地址
     */
    public void sendRegisterEmail(String username, String email) {
        if (!StringUtils.hasLength(username) || !StringUtils.hasLength(email)) {
            log.info("用户名或邮箱为空，不能发送注册验证邮件");
            return;
        }

        String code = UUID.randomUUID().toString().replaceAll("-", "");
        log.info("发送注册验证邮件: username={}, email={}, code={}", username, email, code);

        ResultInfo resultInfo = mailService.sendVerifiedMail(username, email, code);
        log.info(resultInfo.getMessage());

        if (resultInfo.getStatus() == 0) {
            try {
                Map<String, String> userinfo = new HashMap<>();
                userinfo.put("username", username);
                userinfo.put("email", email);
                tempCodeService.hmset(code, userinfo, 3600);
                log.info("保存验证码到 tempCode 成功, code={}", code);
            } catch (Exception e) {
                log.info("保存验证码到 tempCode 失败: {}", e.getMessage());
            }
        } else {
            log.info("发送注册验证邮件失败: {}", resultInfo.getMessage());
        }
    }
}
