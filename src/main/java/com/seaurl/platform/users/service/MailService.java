package com.seaurl.platform.users.service;

import com.seaurl.common.result.ResultInfo;

public interface MailService {
    ResultInfo sendVerifiedMail(String username, String mail, String code);

    ResultInfo resendVerifiedMail(String username, String mail);

    ResultInfo sendRegisterSuccessEMail(String username, String email);

    ResultInfo sendResetPasswordEMail(String email, String code);

    ResultInfo sendUpdateMail(String username, String mail, String code);
}
