package com.xyt.project.service;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * hosts 文件服务
 */
@Slf4j
public class HostsFileService {


    public void updateHostsFile(String hostname, List<String> ipv6Addresses) throws IOException {
        File hostsFile = getHostsFile();
        File tempFile = new File(hostsFile.getAbsolutePath() + ".tmp");

        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(hostsFile));
            writer = new BufferedWriter(new FileWriter(tempFile));

            String line;
            boolean hostnameFound = false;

            // 读取现有内容并更新
            while ((line = reader.readLine()) != null) {
                // 跳过注释和空行
                if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                    writer.write(line + System.lineSeparator());
                    continue;
                }

                // 检查是否包含目标主机名
                if (line.contains(hostname)) {
                    hostnameFound = true;
                    // 跳过这一行（我们将添加新的映射）
                    continue;
                }

                writer.write(line + System.lineSeparator());
            }

            // 添加新的映射
            for (String ip : ipv6Addresses) {
                writer.write(ip + "\t" + hostname + System.lineSeparator());
            }

            // 如果主机名不存在，我们已经在最后添加了它
            if (!hostnameFound) {
                // 添加一个空行分隔
                writer.write(System.lineSeparator());
            }
        } finally {
            // 确保资源被关闭
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("close reader ERROR 更新hosts文件：{}", Throwables.getStackTraceAsString(e));
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.error("close writer ERROR 删除hosts文件：{}", Throwables.getStackTraceAsString(e));
                }
            }
        }

        // 备份原始文件
        File backupFile = new File(hostsFile.getAbsolutePath() + ".bak");
        Files.copy(hostsFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // 用临时文件替换原始文件
        Files.move(tempFile.toPath(), hostsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteHostnameFromHosts(String host, JTextArea consoleArea) throws IOException {
        File hostsFile = getHostsFile();
        File tempFile = new File(hostsFile.getAbsolutePath() + ".tmp");

        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(hostsFile));
            writer = new BufferedWriter(new FileWriter(tempFile));

            String line;
            boolean hostnameFound = false;

            // 读取现有内容并过滤掉指定主机名的记录
            while ((line = reader.readLine()) != null) {
                // 跳过注释和空行
                if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                    writer.write(line + System.lineSeparator());
                    continue;
                }

                // 检查是否包含目标主机名
                if (line.contains(host)) {
                    hostnameFound = true;
                    // 跳过这一行（删除记录）
                    continue;
                }

                writer.write(line + System.lineSeparator());
            }

            if (!hostnameFound) {
                consoleArea.setText("未找到主机名 \"" + host + "\" 的记录");
            }
        } finally {
            // 确保资源被关闭
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("close reader ERROR 删除hosts文件：{}", Throwables.getStackTraceAsString(e));
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.error("close writer ERROR 删除hosts文件：{}", Throwables.getStackTraceAsString(e));
                }
            }
        }

        // 备份原始文件
        File backupFile = new File(hostsFile.getAbsolutePath() + ".bak");
        Files.copy(hostsFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // 用临时文件替换原始文件
        Files.move(tempFile.toPath(), hostsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public String readHostsFile() throws IOException {
        File hostsFile = getHostsFile();
        BufferedReader reader = new BufferedReader(new FileReader(hostsFile));
        StringBuilder content = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            // 过滤掉被注释的行（以 # 开头的行）
            if (!line.trim().startsWith("#")) {
                content.append(line).append("\n");
            }
        }
        reader.close();
        return content.toString();
    }

    public File getHostsFile() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows系统
            return new File(System.getenv("SystemRoot") + "\\System32\\drivers\\etc\\hosts");
        } else {
            // Unix/Linux/Mac系统
            return new File("/etc/hosts");
        }
    }


}
