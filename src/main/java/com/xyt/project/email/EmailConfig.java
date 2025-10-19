package com.xyt.project.email;


/**
 * 邮箱配置信息类
 */

public class EmailConfig {


    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * SMTP服务器
     */
    private String smtpServer;

    /**
     * 端口号
     */
    private String port;

    /**
     * 授权码
     */
    private String authCode;


    /**
     * 是否自动发送
     */
    private Boolean autoSend;


    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public Boolean getAutoSend() {
        return autoSend;
    }

    public void setAutoSend(Boolean autoSend) {
        this.autoSend = autoSend;
    }
}
