package com.seaurl.platform.users.service.impl;


import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.users.config.MailConfig;
import com.seaurl.platform.common.config.ServiceConfig;
import com.seaurl.platform.users.model.MailVerified;
import com.seaurl.platform.users.service.MailService;
import com.seaurl.platform.users.service.MailVerifiedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.UUID;

@Service
public class MailServiceImpl implements MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

    @Autowired
    MailConfig mailConfig;

    @Autowired
    AliyunConfig aliyunConfig;

    @Autowired
    MailVerifiedService mailVerifiedService;

    @Autowired
    ServiceConfig serviceConfig;

    Calendar cal = Calendar.getInstance();

    @Override
    public ResultInfo sendVerifiedMail(String username, String mail, String code) {
        logger.info("host add=>{}", serviceConfig.getHost());
        int year = cal.get(Calendar.YEAR);
        SingleSendMailResponse response = sendEmail(mail,
                "Seaurl ",
                "[Seaurl]请验证您的邮箱",
                "<div style=\"background-color:#ECECEC; padding: 35px;\">\n" +
                        "    <table cellpadding=\"0\" align=\"center\"\n" +
                        "           style=\"width: 600px; margin: 0px auto; text-align: left; position: relative; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 5px; border-bottom-left-radius: 5px; font-size: 14px; font-family:微软雅黑, 黑体; line-height: 1.5; box-shadow: rgb(153, 153, 153) 0px 0px 5px; border-collapse: collapse; background-position: initial initial; background-repeat: initial initial;background:#fff;\">\n" +
                        "        <tbody>\n" +
                        "        <tr>\n" +
                        "            <th valign=\"middle\"\n" +
                        "                style=\"height: 25px; padding: 15px 35px; border-bottom-width: 1px; border-bottom-style: solid; border-bottom-color: #42a3d3; background-color: #54458a; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 0px; border-bottom-left-radius: 0px;\">\n" +
                        "                <font face=\"微软雅黑\" size=\"5\" style=\"color: rgb(255, 255, 255); \">请验证您的邮箱</font>\n" +
                        "            </th>\n" +
                        "        </tr>\n" +
                        "        <tr>\n" +
                        "            <td>\n" +
                        "                <div style=\"padding:25px 35px 40px; background-color:#fff;\">\n" +
                        "                    <h2 style=\"margin: 5px 0px; \">\n" +
                        "                        <font color=\"#333333\" style=\"line-height: 20px; \">\n" +
                        "                            <font style=\"line-height: 22px; \" size=\"4\">\n" +
                        "                                @" + username + "</font>\n" +
                        "                        </font>\n" +
                        "                    </h2>\n" +
                        "                    <p>要完成您的Seaurl注册，我们只需要验证您的电子邮件地址：" + mail + "</p>\n" +
                        "                    <a href='" + serviceConfig.getHost() + "/account/" + username + "/confirm_email_verification/" + code + "'>确认电子邮件地址</a>" +
                        "                    <p align=\"left\">验证之后，您就可以开始使用Seaurl的所有功能来探索。</p>\n" +
//                        "                    <p align=\"left\">2019年06月17号 23时33分20秒</p>\n" +
                        "                    <div style=\"width:700px;margin:0 auto;\">\n" +
                        "                        <div style=\"padding:10px 10px 0;border-top:1px solid #ccc;color:#747474;margin-bottom:20px;line-height:1.3em;font-size:12px;\">\n" +
                        "                            <p>此为系统邮件，请勿回复<br>\n" +
                        "                                请保管好您的邮箱，避免账号被他人盗用\n" +
                        "                            </p>\n" +
                        "                            <p>©Seaurl " + year + "</p>\n" +
                        "                        </div>\n" +
                        "                    </div>\n" +
                        "                </div>\n" +
                        "            </td>\n" +
                        "        </tr>\n" +
                        "        </tbody>\n" +
                        "    </table>\n" +
                        "</div>");
        logger.info("---------- response={} ------------", response);
        if (StringUtils.hasLength(response.getEnvId())) {
            return new ResultInfo("发送邮件成功");
        } else {
            return new ResultInfo(ResultStatus.EMAIL_SEND_FAILURE);
        }
    }

    @Override
    public ResultInfo sendUpdateMail(String username, String mail, String code) {
        logger.info("host add=>{}", serviceConfig.getHost());
        int year = cal.get(Calendar.YEAR);
        SingleSendMailResponse response = sendEmail(mail,
                "Seaurl ",
                "[Seaurl]请验证您的邮箱",
                "<div style=\"background-color:#ECECEC; padding: 35px;\">\n" +
                        "    <table cellpadding=\"0\" align=\"center\"\n" +
                        "           style=\"width: 600px; margin: 0px auto; text-align: left; position: relative; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 5px; border-bottom-left-radius: 5px; font-size: 14px; font-family:微软雅黑, 黑体; line-height: 1.5; box-shadow: rgb(153, 153, 153) 0px 0px 5px; border-collapse: collapse; background-position: initial initial; background-repeat: initial initial;background:#fff;\">\n" +
                        "        <tbody>\n" +
                        "        <tr>\n" +
                        "            <th valign=\"middle\"\n" +
                        "                style=\"height: 25px; padding: 15px 35px; border-bottom-width: 1px; border-bottom-style: solid; border-bottom-color: #42a3d3; background-color: #54458a; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 0px; border-bottom-left-radius: 0px;\">\n" +
                        "                <font face=\"微软雅黑\" size=\"5\" style=\"color: rgb(255, 255, 255); \">请验证您的邮箱</font>\n" +
                        "            </th>\n" +
                        "        </tr>\n" +
                        "        <tr>\n" +
                        "            <td>\n" +
                        "                <div style=\"padding:25px 35px 40px; background-color:#fff;\">\n" +
                        "                    <h2 style=\"margin: 5px 0px; \">\n" +
                        "                        <font color=\"#333333\" style=\"line-height: 20px; \">\n" +
                        "                            <font style=\"line-height: 22px; \" size=\"4\">\n" +
                        "                                @" + username + "</font>\n" +
                        "                        </font>\n" +
                        "                    </h2>\n" +
                        "                    <p>请验证您的电子邮件地址：" + mail + "</p>\n" +
                        "                    <a href='" + serviceConfig.getHost() + "/account/" + username + "/update_email_verification/" + code + "'>确认电子邮件地址</a>" +
                        "                    <div style=\"width:700px;margin:0 auto;\">\n" +
                        "                        <div style=\"padding:10px 10px 0;border-top:1px solid #ccc;color:#747474;margin-bottom:20px;line-height:1.3em;font-size:12px;\">\n" +
                        "                            <p>此为系统邮件，请勿回复<br>\n" +
                        "                                请保管好您的邮箱，避免账号被他人盗用\n" +
                        "                            </p>\n" +
                        "                            <p>©Seaurl " + year + "</p>\n" +
                        "                        </div>\n" +
                        "                    </div>\n" +
                        "                </div>\n" +
                        "            </td>\n" +
                        "        </tr>\n" +
                        "        </tbody>\n" +
                        "    </table>\n" +
                        "</div>");
        logger.info("---------- response={} ------------", response);
        if (StringUtils.hasLength(response.getEnvId())) {
            return new ResultInfo("发送邮件成功");
        } else {
            return new ResultInfo(ResultStatus.EMAIL_SEND_FAILURE);
        }
    }

    @Override
    public ResultInfo resendVerifiedMail(String username, String mail) {
        String code = UUID.randomUUID().toString().replaceAll("-", "");
        // 先删除后添加
        long count = mailVerifiedService.removeByUsername(username);
        if (count > 0) {
            ResultInfo resultInfo = sendVerifiedMail(username, mail, code);
            if (resultInfo.getStatus() == 0) {
                MailVerified mailVerified = new MailVerified(username, mail, code);
                mailVerifiedService.add(mailVerified);
                return new ResultInfo("重新验证邮箱成功");
            }
        }
        return new ResultInfo(ResultStatus.DELETE_ERROR);
    }

    /**
     * 发送注册成功邮件
     *
     * @param username
     * @param email
     * @return
     */
    @Override
    public ResultInfo sendRegisterSuccessEMail(String username, String email) {
        int year = cal.get(Calendar.YEAR);
        SingleSendMailResponse response = sendEmail(email,
                "Seaurl ",
                "[Seaurl]欢迎来到Seaurl @" + username,
                "<div style=\"background-color:#ECECEC; padding: 35px;\">\n" +
                        "    <table cellpadding=\"0\" align=\"center\"\n" +
                        "           style=\"width: 600px; margin: 0px auto; text-align: left; position: relative; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 5px; border-bottom-left-radius: 5px; font-size: 14px; font-family:微软雅黑, 黑体; line-height: 1.5; box-shadow: rgb(153, 153, 153) 0px 0px 5px; border-collapse: collapse; background-position: initial initial; background-repeat: initial initial;background:#fff;\">\n" +
                        "        <tbody>\n" +
                        "        <tr>\n" +
                        "            <th valign=\"middle\"\n" +
                        "                style=\"height: 25px; padding: 15px 35px; border-bottom-width: 1px; border-bottom-style: solid; border-bottom-color: #42a3d3; background-color: #54458a; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 0px; border-bottom-left-radius: 0px;\">\n" +
                        "                <font face=\"微软雅黑\" size=\"5\" style=\"color: rgb(255, 255, 255); \">注册成功！</font>\n" +
                        "            </th>\n" +
                        "        </tr>\n" +
                        "        <tr>\n" +
                        "            <td>\n" +
                        "                <div style=\"padding:25px 35px 40px; background-color:#fff;\">\n" +
                        "                    <h2 style=\"margin: 5px 0px; \">\n" +
                        "                        <font color=\"#333333\" style=\"line-height: 20px; \">\n" +
                        "                            <font style=\"line-height: 22px; \" size=\"4\">\n" +
                        "                                @" + username + "</font>\n" +
                        "                        </font>\n" +
                        "                    </h2>\n" +
                        "                    <h2>欢迎来到<a href='" + serviceConfig.getHost() + "'>Seaurl</a></h2>\n" +
                        "                    <p align=\"left\">快点来跟我们一起分享你的网址吧！</p>\n" +
//                        "                    <p align=\"left\">2019年06月17号 23时33分20秒</p>\n" +
                        "                    <div style=\"width:700px;margin:0 auto;\">\n" +
                        "                        <div style=\"padding:10px 10px 0;border-top:1px solid #ccc;color:#747474;margin-bottom:20px;line-height:1.3em;font-size:12px;\">\n" +
                        "                            <p>此为系统邮件，请勿回复<br>\n" +
                        "                                请保管好您的邮箱，避免账号被他人盗用\n" +
                        "                            </p>\n" +
                        "                            <p>©Seaurl " + year + "</p>\n" +
                        "                        </div>\n" +
                        "                    </div>\n" +
                        "                </div>\n" +
                        "            </td>\n" +
                        "        </tr>\n" +
                        "        </tbody>\n" +
                        "    </table>\n" +
                        "</div>");
        logger.info("---------- response={} ------------", response);
        if (StringUtils.hasLength(response.getEnvId())) {
            return new ResultInfo("发送邮件成功");
        } else {
            return new ResultInfo(ResultStatus.EMAIL_SEND_FAILURE);
        }
    }

    /**
     * 发送重置密码邮件
     *
     * @param email
     * @param code
     * @return
     */
    @Override
    public ResultInfo sendResetPasswordEMail(String email, String code) {
        int year = cal.get(Calendar.YEAR);
        SingleSendMailResponse response = sendEmail(email,
                "Seaurl ",
                "[Seaurl]重置密码",
                "<div style=\"background-color:#ECECEC; padding: 35px;\">\n" +
                        "    <table cellpadding=\"0\" align=\"center\"\n" +
                        "           style=\"width: 600px; margin: 0px auto; text-align: left; position: relative; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 5px; border-bottom-left-radius: 5px; font-size: 14px; font-family:微软雅黑, 黑体; line-height: 1.5; box-shadow: rgb(153, 153, 153) 0px 0px 5px; border-collapse: collapse; background-position: initial initial; background-repeat: initial initial;background:#fff;\">\n" +
                        "        <tbody>\n" +
                        "        <tr>\n" +
                        "            <th valign=\"middle\"\n" +
                        "                style=\"height: 25px; padding: 15px 35px; border-bottom-width: 1px; border-bottom-style: solid; border-bottom-color: #42a3d3; background-color: #54458a; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 0px; border-bottom-left-radius: 0px;\">\n" +
                        "                <font face=\"微软雅黑\" size=\"5\" style=\"color: rgb(255, 255, 255); \">重置你的Seaurl密码</font>\n" +
                        "            </th>\n" +
                        "        </tr>\n" +
                        "        <tr>\n" +
                        "            <td>\n" +
                        "                <div style=\"padding:25px 35px 40px; background-color:#fff;\">\n" +
                        "                    <p>我们听说您丢失了 Seaurl 密码。 不过别担心！ 您可以点击以下链接重置密码：</p>\n" +
                        "                    <a href='" + serviceConfig.getHost() + "/account/password_reset/" + code + "' style=\"text-align: center;padding: 0 0 20px;display: block;\">重置您的密码</a>" +
                        "                    <div style=\"width:700px;margin:0 auto;\">\n" +
                        "                        <div style=\"padding:10px 10px 0;border-top:1px solid #ccc;color:#747474;margin-bottom:20px;line-height:1.3em;font-size:12px;\">\n" +
                        "                            <p>此为系统邮件，请勿回复<br>\n" +
                        "                                请保管好您的邮箱，避免账号被他人盗用\n" +
                        "                            </p>\n" +
                        "                            <p>©Seaurl " + year + "</p>\n" +
                        "                        </div>\n" +
                        "                    </div>\n" +
                        "                </div>\n" +
                        "            </td>\n" +
                        "        </tr>\n" +
                        "        </tbody>\n" +
                        "    </table>\n" +
                        "</div>");
        logger.info("---------- response={} ------------", response);
        if (StringUtils.hasLength(response.getEnvId())) {
            return new ResultInfo("发送邮件成功");
        } else {
            return new ResultInfo(ResultStatus.EMAIL_SEND_FAILURE);
        }
    }

    /**
     * @param email
     * @param alias
     * @param subject
     * @param content
     * @return
     */
    private SingleSendMailResponse sendEmail(String email, String alias, String subject, String content) {
        logger.info("email={},alias={},subject={},content={}", email, alias, subject, content);
        IClientProfile profile = DefaultProfile.getProfile(aliyunConfig.getRegion(), aliyunConfig.getAccessKeyId(), aliyunConfig.getAccessKeySecret());
        IAcsClient client = new DefaultAcsClient(profile);
        SingleSendMailRequest request = new SingleSendMailRequest();
        try {
            request.setAccountName(mailConfig.getMailAddress());
            request.setFromAlias(alias);
            request.setAddressType(1);
//            request.setTagName(aliyunProperties.getEmail().getTagRegister());
            request.setReplyToAddress(true);
            request.setToAddress(email);
            request.setSubject(subject);
            request.setHtmlBody(content);
            request.setMethod(MethodType.POST);
            SingleSendMailResponse httpResponse = client.getAcsResponse(request);
            logger.info("--- httpResponse ={}", httpResponse);
            return httpResponse;
        } catch (com.aliyuncs.exceptions.ClientException e) {
            e.printStackTrace();
            return null;
        }
    }
}
