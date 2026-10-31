package com.seaurl.platform.users.service.email;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.platform.common.cache.TempCodeService;
import com.seaurl.platform.users.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class SendResetPasswordEmailService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MailService mailService;

    @Autowired
    private TempCodeService tempCodeService;

    /**
     * 发送重置密码邮件
     *
     * @param email 邮箱地址
     */
    public void sendResetPasswordEmail(String email) {
        if (!StringUtils.hasLength(email)) {
            log.info("邮箱为空，不能发送重置密码邮件");
            return;
        }

        String code = UUID.randomUUID().toString().replaceAll("-", "");
        log.info("发送重置密码邮件: email={}, code={}", email, code);

        ResultInfo resultInfo = mailService.sendResetPasswordEMail(email, code);
        log.info(resultInfo.getMessage());

        if (resultInfo.getStatus() == 0) {
            try {
                tempCodeService.set(code, email, 3600);
                log.info("保存重置密码 code 到 tempCode 成功, code={}", code);
            } catch (Exception e) {
                log.info("保存重置密码 code 到 tempCode 失败: {}", e.getMessage());
            }
        } else {
            log.info("发送重置密码邮件失败: {}", resultInfo.getMessage());
        }
    }
}
