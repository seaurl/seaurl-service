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
public class SendUpdateEmailService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MailService mailService;

    @Autowired
    private TempCodeService tempCodeService;

    /**
     * 发送修改邮箱验证邮件
     *
     * @param username 用户名
     * @param email    新邮箱地址
     */
    public void sendUpdateEmail(String username, String email) {
        if (!StringUtils.hasLength(username) || !StringUtils.hasLength(email)) {
            log.info("用户名或新邮箱为空，不能发送修改邮箱验证邮件");
            return;
        }

        String code = UUID.randomUUID().toString().replaceAll("-", "");
        log.info("发送修改邮箱验证邮件: username={}, newEmail={}, code={}", username, email, code);

        ResultInfo resultInfo = mailService.sendUpdateMail(username, email, code);
        log.info(resultInfo.getMessage());

        if (resultInfo.getStatus() == 0) {
            try {
                Map<String, String> userinfo = new HashMap<>();
                userinfo.put("username", username);
                userinfo.put("email", email);
                tempCodeService.hmset(code, userinfo, 3600);
                log.info("保存修改邮箱验证码到 tempCode 成功, code={}", code);
            } catch (Exception e) {
                log.info("保存修改邮箱验证码到 tempCode 失败: {}", e.getMessage());
            }
        } else {
            log.info("发送修改邮箱验证邮件失败: {}", resultInfo.getMessage());
        }
    }
}
