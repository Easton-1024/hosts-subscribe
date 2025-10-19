package com.xyt.project.util;

import com.xyt.project.HostsSubscribeApplication;

import java.io.File;

public class AppUtil {
    /**
     * 获取程序安装目录路径
     *
     * @return 程序安装目录路径
     */
    public static String getAppDir() {
        try {
            // 获取当前类的路径
            String path = HostsSubscribeApplication.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();

            // 如果是jar文件，获取其所在目录
            File file = new File(path);
            if (file.isFile()) {
                return file.getParent();
            } else {
                return file.getPath();
            }
        } catch (Exception e) {
            // 如果无法获取，使用用户目录
            return System.getProperty("user.dir");
        }
    }

}
