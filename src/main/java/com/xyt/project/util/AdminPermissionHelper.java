package com.xyt.project.util;

import com.google.common.base.Throwables;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Slf4j
public class AdminPermissionHelper {

    // macOS/Linux的libc接口
    interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);
        int geteuid();
    }

    /**
     * 检查当前进程是否具有管理员/root权限
     * @return true表示有权限，false表示无权限
     */
    public static boolean isAdmin() {
        if (Platform.isWindows()) {
            try {
                // 获取当前进程令牌
                WinNT.HANDLEByReference hToken = new WinNT.HANDLEByReference();
                boolean success = Advapi32.INSTANCE.OpenProcessToken(
                        Kernel32.INSTANCE.GetCurrentProcess(),
                        WinNT.TOKEN_QUERY,
                        hToken
                );

                if (!success) {
                    return false;
                }

                // 查询令牌权限信息
                WinNT.TOKEN_ELEVATION elevation = new WinNT.TOKEN_ELEVATION();
                IntByReference size = new IntByReference();
                success = Advapi32.INSTANCE.GetTokenInformation(
                        hToken.getValue(),
                        WinNT.TOKEN_INFORMATION_CLASS.TokenElevation,
                        elevation,
                        elevation.size(),
                        size
                );

                // 关闭令牌句柄
                Kernel32.INSTANCE.CloseHandle(hToken.getValue());

                return success && elevation.TokenIsElevated > 0;
            } catch (Exception e) {
                log.error("获取当前进程权限失败:{}" , Throwables.getStackTraceAsString(e));
                return false;
            }
        } else {
            return CLibrary.INSTANCE.geteuid() == 0;
        }
    }

    /**
     * 以管理员权限执行hosts文件更新操作
     * @param hostname 主机名
     * @param ipv6Addresses IPv6地址列表
     * @throws IOException 操作失败时抛出
     */
    public static void updateHostsWithAdminPrivileges(String hostname, List<String> ipv6Addresses) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            updateHostsWindows(hostname, ipv6Addresses);
        } else if (os.contains("mac")) {
            updateHostsMac(hostname, ipv6Addresses);
        }
    }

    /**
     * 以管理员权限执行hosts文件删除操作
     * @param hostname 主机名
     * @throws IOException 操作失败时抛出
     */
    public static void deleteHostsWithAdminPrivileges(String hostname) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            deleteHostsWindows(hostname);
        } else if (os.contains("mac")) {
            deleteHostsMac(hostname);
        }
    }

    private static void updateHostsWindows(String hostname, List<String> ipv6Addresses) throws IOException {
        try {
            // 创建PowerShell脚本内容
            StringBuilder psScript = new StringBuilder();
            psScript.append("try {\n");
            psScript.append("  $content = Get-Content -Path \"$env:SystemRoot\\System32\\drivers\\etc\\hosts\"\n");
            psScript.append("  $newContent = $content | Where-Object { $_ -notlike \"*$hostname*\" }\n");

            // 添加新的映射
            for (String ip : ipv6Addresses) {
                psScript.append("  $newContent += \"").append(ip).append("    ").append(hostname).append("\"\n");
            }

            psScript.append("  $newContent | Out-File -FilePath \"$env:SystemRoot\\System32\\drivers\\etc\\hosts\" -Encoding UTF8\n");
            psScript.append("  Write-Output \"success\"\n");
            psScript.append("} catch {\n");
            psScript.append("  Write-Error \"$_\"\n");
            psScript.append("}\n");

            // 写入临时脚本文件
            File tempScript = File.createTempFile("updateHosts", ".ps1");
            tempScript.deleteOnExit();
            Files.write(tempScript.toPath(), psScript.toString().getBytes("UTF-8"));

            // 执行PowerShell脚本
            String[] cmd = {
                    "powershell.exe", "-ExecutionPolicy", "Bypass", "-File", tempScript.getAbsolutePath()
            };

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("PowerShell脚本执行失败");
            }
        } catch (Exception e) {
            throw new IOException("更新hosts文件失败: " + e.getMessage(), e);
        }
    }

    private static void deleteHostsWindows(String hostname) throws IOException {
        try {
            // 创建PowerShell脚本内容
            StringBuilder psScript = new StringBuilder();
            psScript.append("try {\n");
            psScript.append("  $content = Get-Content -Path \"$env:SystemRoot\\System32\\drivers\\etc\\hosts\"\n");
            psScript.append("  $newContent = $content | Where-Object { $_ -notlike \"*$hostname*\" }\n");
            psScript.append("  $newContent | Out-File -FilePath \"$env:SystemRoot\\System32\\drivers\\etc\\hosts\" -Encoding UTF8\n");
            psScript.append("  Write-Output \"success\"\n");
            psScript.append("} catch {\n");
            psScript.append("  Write-Error \"$_\"\n");
            psScript.append("}\n");

            // 写入临时脚本文件
            File tempScript = File.createTempFile("deleteHosts", ".ps1");
            tempScript.deleteOnExit();
            Files.write(tempScript.toPath(), psScript.toString().getBytes("UTF-8"));

            // 执行PowerShell脚本
            String[] cmd = {
                    "powershell.exe", "-ExecutionPolicy", "Bypass", "-File", tempScript.getAbsolutePath()
            };

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("PowerShell脚本执行失败");
            }
        } catch (Exception e) {
            throw new IOException("删除hosts记录失败: " + e.getMessage(), e);
        }
    }

    private static void updateHostsMac(String hostname, List<String> ipv6Addresses) throws IOException {
        try {
            // 创建临时脚本文件
            File scriptFile = File.createTempFile("hosts_editor_", ".sh");
            scriptFile.deleteOnExit();
            scriptFile.setExecutable(true);

            // 写入脚本内容
            StringBuilder scriptContent = new StringBuilder();
            scriptContent.append("#!/bin/bash\n");
            scriptContent.append("grep -v \"").append(hostname).append("\" /etc/hosts > /tmp/hosts.tmp\n");

            // 添加新的映射
            for (String ip : ipv6Addresses) {
                scriptContent.append("echo \"").append(ip).append("    ").append(hostname)
                        .append("\" >> /tmp/hosts.tmp\n");
            }

            scriptContent.append("cp /tmp/hosts.tmp /etc/hosts\n");
            scriptContent.append("rm /tmp/hosts.tmp\n");

            Files.write(scriptFile.toPath(), scriptContent.toString().getBytes());

            // 使用osascript请求管理员权限执行脚本
            String[] cmd = {
                    "osascript", "-e",
                    "do shell script \"bash " + scriptFile.getAbsolutePath() + "\" with administrator privileges"
            };

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("管理员权限请求被拒绝或执行失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("操作被中断", e);
        } catch (Exception e) {
            throw new IOException("更新hosts文件失败: " + e.getMessage(), e);
        }
    }

    private static void deleteHostsMac(String hostname) throws IOException {
        try {
            // 创建临时脚本文件
            File scriptFile = File.createTempFile("hosts_editor_", ".sh");
            scriptFile.deleteOnExit();
            scriptFile.setExecutable(true);

            // 写入脚本内容
            StringBuilder scriptContent = new StringBuilder();
            scriptContent.append("#!/bin/bash\n");
            scriptContent.append("grep -v \"").append(hostname).append("\" /etc/hosts > /tmp/hosts.tmp\n");
            scriptContent.append("cp /tmp/hosts.tmp /etc/hosts\n");
            scriptContent.append("rm /tmp/hosts.tmp\n");

            Files.write(scriptFile.toPath(), scriptContent.toString().getBytes());

            // 使用osascript请求管理员权限执行脚本
            String[] cmd = {
                    "osascript", "-e",
                    "do shell script \"bash " + scriptFile.getAbsolutePath() + "\" with administrator privileges"
            };

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("管理员权限请求被拒绝或执行失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("操作被中断", e);
        } catch (Exception e) {
            throw new IOException("删除hosts记录失败: " + e.getMessage(), e);
        }
    }
}
