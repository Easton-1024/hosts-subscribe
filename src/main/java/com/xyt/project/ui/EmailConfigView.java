package com.xyt.project.ui;

import cn.hutool.core.util.StrUtil;
import com.google.common.base.Throwables;
import com.xyt.project.config.AppConfiguration;
import com.xyt.project.email.EmailConfig;
import com.xyt.project.email.EmailSender;
import com.xyt.project.email.EmailService;
import com.xyt.project.model.IPInfoDTO;
import com.xyt.project.service.NetworkService;
import com.xyt.project.util.AppUtil;
import lombok.extern.slf4j.Slf4j;

import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EmailConfigView {


    private JFrame parentFrame;

    private EmailService emailService;

    private NetworkService networkService;

    private JDialog emailDialog;


    /**
     * 设备名称输入框
     */
    private JTextField deviceNameField;

    /**
     * 邮箱地址输入框
     */
    private JTextField emailField;

    /**
     * SMTP服务器输入框
     */
    private JTextField smtpServerField;

    /**
     * 端口输入框
     */
    private JTextField portField;

    /**
     * 授权码输入框
     */
    private JPasswordField authCodeField;

    /**
     * 自动发送IPv6地址变更通知复选框
     **/
    private JCheckBox autoSendCheckBox;


    public EmailConfigView(JFrame parentFrame, NetworkService networkService) {
        this.parentFrame = parentFrame;
        emailService = new EmailService();
        this.networkService = networkService;
    }

    public EmailService getEmailService() {
        return emailService;
    }

    public EmailSender getEmailSender() {
        EmailConfig config = getCurrentEmailConfig();
        return new EmailSender(config);
    }


    /**
     * 邮箱通知按钮事件处理方法
     */
    public void initEmailView() {
        String appDir = AppUtil.getAppDir();
        EmailConfig config = emailService.loadEmailConfig(appDir);
        String deviceName;
        try {
            deviceName = StrUtil.isBlank(config.getDeviceName()) ? java.net.InetAddress.getLocalHost().getHostName() : config.getDeviceName();
        } catch (java.net.UnknownHostException e) {
            deviceName = "Unknown";
        }
        String email = StrUtil.isBlank(config.getEmail()) ? "" : config.getEmail();
        String smtpServer = StrUtil.isBlank(config.getSmtpServer()) ? "" : config.getSmtpServer();
        String port = StrUtil.isBlank(config.getPort()) ? "465" : config.getPort();
        String authCode = StrUtil.isBlank(config.getAuthCode()) ? "" : config.getAuthCode();


        // 创建邮箱通知对话框
        emailDialog = new JDialog(parentFrame, "配置订阅邮箱", true);
        emailDialog.setSize(500, 400); // 增加高度以容纳新控件
        emailDialog.setLocationRelativeTo(parentFrame);
        emailDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 创建主面板
        JPanel emailPanel = new JPanel(new BorderLayout(10, 10));
        emailPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 创建表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 本机设备名称
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("本机设备名称:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        deviceNameField = new JTextField(20);
        // 设置默认设备名称为本机主机名

        deviceNameField.setText(deviceName);
        formPanel.add(deviceNameField, gbc);

        // 邮箱地址
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(new JLabel("邮箱地址:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        emailField = new JTextField(20);
        emailField.setText(email);
        formPanel.add(emailField, gbc);

        // 收件箱域名
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        formPanel.add(new JLabel("SMTP服务器:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        smtpServerField = new JTextField(20);
        smtpServerField.setText(smtpServer);
        formPanel.add(smtpServerField, gbc);

        // 端口
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        formPanel.add(new JLabel("端口:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        portField = new JTextField(20);
        portField.setText(port); // 默认端口
        formPanel.add(portField, gbc);

        // 授权码
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        formPanel.add(new JLabel("授权码:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        authCodeField = new JPasswordField(20);
        authCodeField.setText(authCode);
        formPanel.add(authCodeField, gbc);

        // 自动发送复选框
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2; // 跨两列
        autoSendCheckBox = new JCheckBox("自动发送IPv6地址变更通知");
        autoSendCheckBox.setSelected(config.getAutoSend()); // 默认选中
        formPanel.add(autoSendCheckBox, gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        JButton testButton = new JButton("测试发送");

        // 测试发送按钮事件
        testButton.addActionListener(e -> testSendEmailAction());

        // 确定按钮事件
        confirmButton.addActionListener(e -> confirmEmailConfigAction());

        // 取消按钮事件
        cancelButton.addActionListener(e -> emailDialog.dispose());

        buttonPanel.add(testButton);
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        // 添加组件到面板
        emailPanel.add(formPanel, BorderLayout.CENTER);
        emailPanel.add(buttonPanel, BorderLayout.SOUTH);

        emailDialog.add(emailPanel);
        emailDialog.setVisible(true);


    }

    private void setPanelEnable(EmailConfig config) {
        deviceNameField.setText(config.getDeviceName());
        emailField.setText(config.getEmail());
        smtpServerField.setText(config.getSmtpServer());
        portField.setText(config.getPort());
        authCodeField.setText(config.getAuthCode());
        autoSendCheckBox.setSelected(config.getAutoSend());
    }


    public EmailConfig getCurrentEmailConfig() {
        String deviceName = deviceNameField.getText().trim();
        String email = emailField.getText().trim();
        String smtpServer = smtpServerField.getText().trim();
        String port = portField.getText().trim();
        String authCode = new String(authCodeField.getPassword());
        boolean autoSend = autoSendCheckBox.isSelected();

        // 创建临时邮箱配置用于测试
        EmailConfig config = new EmailConfig();
        config.setDeviceName(deviceName);
        config.setEmail(email);
        config.setSmtpServer(smtpServer);
        config.setPort(port);
        config.setAuthCode(authCode);
        config.setAutoSend(autoSend);
        return config;
    }


    public String getEmailSendContent(String deviceName, List<IPInfoDTO> ipList) {
        String templateStr = "网卡：{}{}：{} {}";
        List<String> linList = new ArrayList<>();
        for (IPInfoDTO ipInfoDTO : ipList) {
            String format = StrUtil.format(templateStr, ipInfoDTO.getNetworkName(), ipInfoDTO.getAddressType(), ipInfoDTO.getIpAddress(), deviceName);
            linList.add(format);
        }
        return StrUtil.join("\n", linList);
    }


    private boolean checkConfig() {
        String deviceName = deviceNameField.getText().trim();
        String email = emailField.getText().trim();
        String smtpServer = smtpServerField.getText().trim();
        String port = portField.getText().trim();
        String authCode = new String(authCodeField.getPassword());

        if (deviceName.isEmpty() || email.isEmpty() || smtpServer.isEmpty() || port.isEmpty() || authCode.isEmpty()) {
            JOptionPane.showMessageDialog(emailDialog, "请填写所有字段", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!isValidDomain(deviceName)) {
            JOptionPane.showMessageDialog(emailDialog, "设备名称格式不正确", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(emailDialog, "请输入有效的邮箱地址", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            int portNum = Integer.parseInt(port);
            if (portNum <= 0 || portNum > 65535) {
                JOptionPane.showMessageDialog(emailDialog, "端口号必须在1-65535之间", "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(emailDialog, "请输入有效的端口号", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    // 新增域名验证方法
    private boolean isValidDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        // 域名验证正则表达式
        String domainRegex = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
        return domain.matches(domainRegex);
    }


    private void confirmEmailConfigAction() {
        if (!checkConfig()) {
            return;
        }
        EmailConfig currentEmailConfig = getCurrentEmailConfig();
        saveEmailConfig(currentEmailConfig);
        JOptionPane.showMessageDialog(emailDialog, "邮箱配置已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
        AppConfiguration.setEmailConfig(currentEmailConfig);
        emailDialog.dispose();
    }


    private void testSendEmailAction() {
        if (!checkConfig()) {
            return;
        }
        EmailConfig config = getCurrentEmailConfig();
        // 执行测试发送
        testEmailSending(config, emailDialog);

    }

    /**
     * 测试邮件发送功能
     *
     * @param config 邮箱配置
     * @param parent 父级对话框
     */
    private void testEmailSending(EmailConfig config, JDialog parent) {
        // 在后台线程中执行邮件发送，避免阻塞UI
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // 这里调用邮件服务发送测试邮件
                    // 由于没有看到EmailService的具体实现，我们假设有一个sendTestEmail方法
                    // emailService.sendTestEmail(config);

                    // 模拟邮件发送过程
                    List<IPInfoDTO> ipList = networkService.getLocalNetworkAddressList();
                    EmailConfig currentViewEmailConfig = getCurrentEmailConfig();
                    sendEmail(currentViewEmailConfig, ipList);
                    return true;
                } catch (Exception e) {
                    log.info("邮件发送失败：{}", Throwables.getStackTraceAsString(e));
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(parent, "测试邮件发送成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(parent, "测试邮件发送失败，请检查配置！", "失败", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(parent, "测试邮件发送过程中发生错误：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        // 显示正在发送的提示
        JOptionPane.showMessageDialog(parent, "正在发送测试邮件，请稍候...", "提示", JOptionPane.INFORMATION_MESSAGE);
        worker.execute();
    }

    /**
     * 验证邮箱地址格式
     *
     * @param email 邮箱地址
     * @return 是否为有效邮箱地址
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // 简单的邮箱格式验证
        String emailRegex = "^[\\w.-]+@([\\w-]+\\.)+[\\w-]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * 保存邮箱配置到文件
     *
     * @param config 邮箱配置信息
     */
    private void saveEmailConfig(EmailConfig config) {
        try {
            String appDir = AppUtil.getAppDir();
            emailService.saveEmailConfig(config, appDir);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentFrame, "保存邮箱配置时出错: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void sendEmail(EmailConfig emailConfig, List<IPInfoDTO> ipList) throws MessagingException {
        EmailSender emailSender = new EmailSender(emailConfig);
        String content = getEmailSendContent(emailConfig.getDeviceName(), ipList);
        String fromName = "主机IP变更通知";
        emailSender.sendSimpleEmail(fromName, emailConfig.getEmail(), emailConfig.getDeviceName(), content);
    }


}
