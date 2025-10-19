package com.xyt.project.email;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class EmailService {


    private static final String configFileName = "email_config.properties";

    /**
     * 从文件读取邮箱配置
     *
     * @return 邮箱配置信息
     */
    public EmailConfig loadEmailConfig(String configDir) {
        EmailConfig config = new EmailConfig();
        config.setAutoSend(false);
        try {
            File configFile = new File(configDir, configFileName);

            if (configFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                    config.setDeviceName(props.getProperty("deviceName", ""));
                    config.setEmail(props.getProperty("email", ""));
                    config.setSmtpServer(props.getProperty("smtp.server", ""));
                    config.setPort(props.getProperty("port", ""));
                    config.setAuthCode(props.getProperty("auth.code", ""));
                    config.setAutoSend(Boolean.parseBoolean(props.getProperty("auto.send", "false")));
                }
            }
        } catch (IOException e) {
            log.error("读取邮箱配置文件失败：{}" , Throwables.getStackTraceAsString(e));
        }
        return config;
    }


    /**
     * 保存邮箱配置
     * @param config
     * @param configDir
     * @throws IOException
     */
    public void saveEmailConfig(EmailConfig config,String configDir) throws IOException {
        File configFile = new File(configDir, configFileName);
        Properties props = new Properties();
        props.setProperty("deviceName", config.getDeviceName() != null ? config.getDeviceName() : "");
        props.setProperty("email", config.getEmail() != null ? config.getEmail() : "");
        props.setProperty("smtp.server", config.getSmtpServer() != null ? config.getSmtpServer() : "");
        props.setProperty("port", config.getPort() != null ? config.getPort() : "");
        // 注意：出于安全考虑，授权码可能不应该明文保存
        props.setProperty("auth.code", config.getAuthCode() != null ? config.getAuthCode() : "");
        props.setProperty("auto.send", config.getAutoSend() ? "true" : "false");

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Email Configuration");
        }
    }


}
