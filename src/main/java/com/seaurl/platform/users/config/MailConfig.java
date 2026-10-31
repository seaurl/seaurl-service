package com.seaurl.platform.users.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class MailConfig {
    @Value("${mail.address}")
    private String mailAddress = "";

    public String getMailAddress() {
        return mailAddress;
    }

    @Value("${mail.smtp.password}")
    private String mailPassword = "";

    public String getMailPassword() {
        return mailPassword;
    }
}
