package com.xyt.project.config;

import com.xyt.project.email.EmailConfig;
import com.xyt.project.email.EmailService;
import com.xyt.project.util.AppUtil;

public class AppConfiguration {


    private static   EmailConfig emailConfig;


    public AppConfiguration(EmailService emailService) {
        emailConfig = emailService.loadEmailConfig(AppUtil.getAppDir());
    }


    public static EmailConfig getEmailConfig() {
        return emailConfig;
    }


    public static void setEmailConfig(EmailConfig emailConfig) {
        AppConfiguration.emailConfig = emailConfig;
    }


}
