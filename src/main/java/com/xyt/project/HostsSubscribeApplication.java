package com.xyt.project;


import cn.hutool.core.util.StrUtil;
import com.google.common.base.Throwables;
import com.xyt.project.config.AppConfiguration;
import com.xyt.project.listener.CopyContentFilter;
import com.xyt.project.model.IPInfoDTO;
import com.xyt.project.model.IPv6Address;
import com.xyt.project.service.HostsFileService;
import com.xyt.project.service.NetworkService;
import com.xyt.project.ui.EmailConfigView;
import com.xyt.project.util.AdminPermissionHelper;
import com.xyt.project.util.AppUtil;
import lombok.extern.slf4j.Slf4j;

import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 本地主机管理器
 */
@Slf4j
public class HostsSubscribeApplication extends JFrame {


    /**
     * 主面板
     */
    private JPanel mainPanel;

    /**
     * 工作面板
     * 包括 配置 hosts 面板和控制台面板
     */
    private JPanel workPanel;

    /**
     * 配置 hosts 面板
     */
    private JPanel configHostsPanel;

    private JTextField hostField;


    private JTextArea consoleArea;


    private HostsFileService hostsFileService;

    private NetworkService networkService;

    private AppConfiguration appConfiguration;

    private CopyContentFilter copyContentFilter;

    private EmailConfigView emailConfigView;

    /**
     * 定时检查IP变更的任务
     */
    private Timer ipCheckTimer;


    public HostsSubscribeApplication() {
        log.info("初始化程序");
        hostsFileService = new HostsFileService();
        networkService = new NetworkService();
        copyContentFilter = new CopyContentFilter();
        emailConfigView = new EmailConfigView(this, networkService);
        this.appConfiguration = new AppConfiguration(emailConfigView.getEmailService());
        initWindow();
        initSystemTray();
        // 启动IP检查定时器
        startIPCheckTimer();

    }

    public static void main(String[] args) {
        // 设置外观和感觉
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.info("初始化程序异常：{}", Throwables.getStackTraceAsString(e));
        }

        SwingUtilities.invokeLater(() -> {
            HostsSubscribeApplication editor = new HostsSubscribeApplication();
            editor.setVisible(true);
        });
    }


    public void initWindow() {

        initTitle();
        initMainPanel();
        initToolPanel();
        initWorkPanel();
        configHostsPanel();
        initInputPanel();
        initConsolePanel();
        initEndPanel();
    }

    private void initEndPanel() {
        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // 新增的退出程序按钮
        JButton exitButton = new JButton("退出程序");
        exitButton.addActionListener(e -> exit());
        endPanel.add(exitButton);

        // 将网卡信息面板添加到主面板的北面
        mainPanel.add(endPanel, BorderLayout.SOUTH);
    }

    private void startIPCheckTimer() {
        // 创建一个定时任务，每10秒执行一次
        ipCheckTimer = new Timer(10000, e -> notifyIPChange());
        ipCheckTimer.start();
    }

    private void stopIPCheckTimer() {
        if (ipCheckTimer != null && ipCheckTimer.isRunning()) {
            ipCheckTimer.stop();
        }
    }

    private void notifyIPChange() {
        new Thread(() -> {
            List<IPInfoDTO> ipInfoDTOS = networkService.filterIPList();
            if ((!AppConfiguration.getEmailConfig().getAutoSend()) || ipInfoDTOS.isEmpty()) {
                return;
            }
            boolean isSendSuccess = false;
            try {
                emailConfigView.sendEmail(AppConfiguration.getEmailConfig(), ipInfoDTOS);
                isSendSuccess = true;
            } catch (MessagingException e) {
                log.error("发送邮件异常：{}", Throwables.getStackTraceAsString(e));
            }finally {
                if (isSendSuccess) {
                    networkService.saveLocalIP(AppUtil.getAppDir());
                }
            }
        }).start();

    }


    /**
     * 初始化标题
     */
    private void initTitle() {
        setTitle("主机订阅管理");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // 加载图标
        Image icon = Toolkit.getDefaultToolkit().createImage("image/icon.png"); // 确保路径正确
        if (icon != null) {
            setIconImage(icon); // 设置窗口图标
        }
        // 添加窗口关闭监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        // 添加窗口状态变化监听器
        addWindowStateListener(e -> {
            // 当窗口状态变为最小化时，隐藏窗口并显示在系统托盘中
            if (e.getNewState() == Frame.ICONIFIED) {
                log.info("窗口最小化");
                // 隐藏窗口而不是仅仅最小化
                setVisible(false);
            }

        });

        setSize(1024, 720);
        setLocationRelativeTo(null);
    }

    // 在构造函数中调用此方法初始化系统托盘
    private void initSystemTray() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            // 创建托盘图标
            Image image = Toolkit.getDefaultToolkit().createImage("image/icon.png"); // 替换为实际图标路径
            TrayIcon trayIcon = new TrayIcon(image, "Hosts文件编辑器");
            trayIcon.setImageAutoSize(true);

            // 添加鼠标监听器，单击显示窗口，双击恢复窗口
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        // 单击托盘图标显示窗口
                        setVisible(true);
                        setExtendedState(Frame.NORMAL);
                    } else if (e.getClickCount() == 2) {
                        // 双击托盘图标恢复窗口
                        setVisible(true);
                        setExtendedState(Frame.NORMAL);
                    }
                }
            });

            // 添加右键菜单
            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Open");
            openItem.addActionListener(e -> {
                setVisible(true);
                setExtendedState(Frame.NORMAL);
            });
            popup.add(openItem);

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                // 停止IP检查定时器
                stopIPCheckTimer();
                // 退出程序
                System.exit(0);
            });
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);


            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error("添加系统托盘图标失败: {}", Throwables.getStackTraceAsString(e));
            }
        }
    }


    /**
     * 初始化主面板
     */
    private void initMainPanel() {
        // 创建主面板
        this.mainPanel = new JPanel(new BorderLayout(10, 10));
        this.mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        add(mainPanel);
    }

    /**
     * 初始化工作面板
     */
    private void initWorkPanel() {
        this.workPanel = new JPanel(new BorderLayout());
        mainPanel.add(workPanel, BorderLayout.CENTER);
    }


    /**
     * 初始化工具面板
     */
    private void initToolPanel() {
        JPanel toolPanel = new JPanel(new BorderLayout());
        toolPanel.setBorder(BorderFactory.createTitledBorder("工具"));

        // 网卡按钮面板

        JPanel toolsButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton emailNotifications = new JButton("邮箱订阅设置");
        emailNotifications.addActionListener(e -> emailConfigView.initEmailView()); // 添加事件监听器
        toolsButtonPanel.add(emailNotifications);

        JButton displayIpButton = new JButton("显示本机IP信息");
        displayIpButton.addActionListener(e -> refreshAction());
        toolsButtonPanel.add(displayIpButton);

        JButton viewButton = new JButton("查看hosts文件");
        viewButton.addActionListener(e -> viewAction());
        toolsButtonPanel.add(viewButton);


        toolPanel.add(toolsButtonPanel, BorderLayout.SOUTH);

        // 将网卡信息面板添加到主面板的北面
        mainPanel.add(toolPanel, BorderLayout.NORTH);

    }

    private void configHostsPanel() {
        configHostsPanel = new JPanel(new BorderLayout());
        configHostsPanel.setBorder(BorderFactory.createTitledBorder("配置hosts"));
        workPanel.add(configHostsPanel, BorderLayout.NORTH);

    }


    /**
     * 输入面板
     */
    private void initInputPanel() {

        // 创建输入面板
        JPanel inputHostPanel = new JPanel(new GridBagLayout());
        configHostsPanel.add(inputHostPanel, BorderLayout.NORTH);

        // 初始化边距
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(30, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 主机名标签和输入框区域
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputHostPanel.add(new JLabel("host:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        hostField = new JTextField(20);
        hostField.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        hostField.setToolTipText("请输入主机名和IPv6地址");

        inputHostPanel.add(hostField, gbc);


        // 配置保存按钮
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveAction());
        buttonPanel.add(saveButton);

        // 删除按钮
        JButton deleteButton = new JButton("删除");
        deleteButton.addActionListener(e -> deleteAction());
        buttonPanel.add(deleteButton);

        // 将按钮面板添加到主面板的南部
        configHostsPanel.add(buttonPanel);


    }


    /**
     * 控制台面板
     */
    private void initConsolePanel() {
        // 创建控制台面板
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBorder(BorderFactory.createTitledBorder("控制台"));

        // 创建文本区域用于显示结果
        consoleArea = new JTextArea(10, 20);
        consoleArea.setFont(new Font(Font.DIALOG, Font.PLAIN, 18));
        consoleArea.setEditable(false);
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);


        // 添加滚动条
        JScrollPane scrollPane = new JScrollPane(consoleArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        consolePanel.add(scrollPane, BorderLayout.CENTER);

        // 将控制台面板添加到主面板的中央
        workPanel.add(consolePanel, BorderLayout.CENTER);
    }


    // 查看按钮事件处理方法
    private void viewAction() {
        try {
            String content = hostsFileService.readHostsFile();

            consoleArea.setText("hosts文件内容:\n\n" + content);
        } catch (IOException e) {
            consoleArea.setText("读取hosts文件时出错: " + e.getMessage());
            JOptionPane.showMessageDialog(HostsSubscribeApplication.this, "无法读取hosts文件: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }

    }

    /**
     * 刷新
     */
    private void refreshAction() {
        List<IPInfoDTO> ipList = networkService.getLocalNetworkAddressList();
        List<String> linList = new ArrayList<>();
        for (IPInfoDTO ipInfoDTO : ipList) {
            String displayName = ipInfoDTO.getNetworkName();
            String ipType = ipInfoDTO.getIpType();
            String addressType = ipInfoDTO.getAddressType();
            String hostAddress = ipInfoDTO.getIpAddress();
            String format = StrUtil.format("网卡名称：{}，{}地址{}：{}", displayName, ipType, addressType, hostAddress);
            linList.add(format);
        }
        consoleArea.setText(StrUtil.join("\n", linList));
    }


    // 保存按钮事件处理方法
    private void saveAction() {
        String hostConfig = hostField.getText().trim();

        // 验证输入
        if (hostConfig.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入主机配置", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 分割行内容，考虑多个空格的情况
        String[] configParts = hostConfig.split("\\s+");

        // 至少需要IP地址和主机名
        if (configParts.length < 2) {
            JOptionPane.showMessageDialog(this, "请输入正确的配置格式：IP地址 主机名 [别名1] [别名2]...", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String ip = configParts[0].trim();
        String hostname = configParts[1].trim();

        // 验证IP地址格式（支持IPv4和IPv6）
        if (!isValidIpAddress(ip)) {
            JOptionPane.showMessageDialog(this, "无效的IP地址: " + ip, "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 验证主机名格式
        if (!isValidHostname(hostname)) {
            JOptionPane.showMessageDialog(this, "无效的主机名: " + hostname, "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 收集所有主机名（包括别名）
        List<String> hostnames = new ArrayList<>();
        for (int i = 1; i < configParts.length; i++) {
            String name = configParts[i].trim();
            if (!name.isEmpty()) {
                if (isValidHostname(name)) {
                    hostnames.add(name);
                } else {
                    JOptionPane.showMessageDialog(this, "无效的主机名: " + name, "输入错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        // 构建hosts文件条目
        String hostsEntry = ip + " " + String.join(" ", hostnames);

        // 更新hosts文件
        try {
            // 检查是否具有管理员权限
            if (!AdminPermissionHelper.isAdmin()) {
                // 请求管理员权限执行更新操作
                AdminPermissionHelper.updateHostsWithAdminPrivileges(hostname, Collections.singletonList(ip));
                consoleArea.setText("成功更新hosts文件:\n" + hostsEntry);
            } else {
                // 已有管理员权限，直接更新
                hostsFileService.updateHostsFile(hostname, Collections.singletonList(ip));
                consoleArea.setText("成功更新hosts文件:\n" + hostsEntry);
            }
        } catch (IOException ex) {
            consoleArea.setText("更新hosts文件时出错: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "无法更新hosts文件: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 验证主机名格式
     *
     * @param hostname 主机名字符串
     * @return 是否为有效主机名
     */
    private boolean isValidHostname(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return false;
        }

        // 主机名验证规则：
        // 1. 长度不能超过253个字符
        // 2. 只能包含字母、数字、连字符和点
        // 3. 不能以连字符或点开头或结尾
        // 4. 不能包含连续的点
        if (hostname.length() > 253) {
            return false;
        }

        String hostnameRegex = "^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*$";
        return hostname.matches(hostnameRegex);
    }

    /**
     * 验证IP地址格式（支持IPv4和IPv6）
     *
     * @param ip IP地址字符串
     * @return 是否为有效IP地址
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // IPv4地址验证
        if (ip.contains(".")) {
            return isValidIPv4Address(ip);
        }

        // IPv6地址验证
        if (ip.contains(":")) {
            return IPv6Address.isValidIPv6(ip);
        }

        return false;
    }

    /**
     * 验证IPv4地址格式
     *
     * @param ip IPv4地址字符串
     * @return 是否为有效IPv4地址
     */
    private boolean isValidIPv4Address(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }


    // 删除按钮事件处理方法
    private void deleteAction() {
        String host = hostField.getText().trim();

        // 验证输入
        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入要删除的主机名", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 确认删除操作
        int option = JOptionPane.showConfirmDialog(
                this,
                "确定要删除主机名 \"" + host + "\" 的记录吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            try {
                // 检查是否具有管理员权限
                if (!AdminPermissionHelper.isAdmin()) {
                    // 请求管理员权限执行删除操作
                    AdminPermissionHelper.deleteHostsWithAdminPrivileges(host);
                    consoleArea.setText("成功删除主机名 \"" + host + "\" 的记录");
                    // 清空输入框
                    hostField.setText("");
                } else {
                    // 已有管理员权限，直接删除
                    hostsFileService.deleteHostnameFromHosts(host, consoleArea);
                    consoleArea.setText("成功删除主机名 \"" + host + "\" 的记录");
                    // 清空输入框
                    consoleArea.setText("");
                }
            } catch (IOException ex) {
                consoleArea.setText("删除记录时出错: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "无法删除记录: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void exit() {
        ImageIcon customIcon = new ImageIcon("image/questionMark.png");
        // 弹出确认对话框
        int option = JOptionPane.showConfirmDialog(
                HostsSubscribeApplication.this,
                "确定要退出程序吗？",
                "确认退出",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                customIcon
        );

        if (option == JOptionPane.YES_OPTION) {
            // 停止IP检查定时器
            stopIPCheckTimer();
            // 退出程序
            System.exit(0);
        }

    }


}
