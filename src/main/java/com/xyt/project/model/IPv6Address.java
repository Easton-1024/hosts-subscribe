package com.xyt.project.model;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class IPv6Address {


    private static final Pattern IPV6_PATTERN = Pattern.compile("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" + "^::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|" + "^[0-9a-fA-F]{1,4}::([0-9a-fA-F]{1,4}:){0,5}[0-9a-fA-F]{1,4}$|" + "^([0-9a-fA-F]{1,4}:){2}:([0-9a-fA-F]{1,4}:){0,4}[0-9a-fA-F]{1,4}$|" + "^([0-9a-fA-F]{1,4}:){3}:([0-9a-fA-F]{1,4}:){0,3}[0-9a-fA-F]{1,4}$|" + "^([0-9a-fA-F]{1,4}:){4}:([0-9a-fA-F]{1,4}:){0,2}[0-9a-fA-F]{1,4}$|" + "^([0-9a-fA-F]{1,4}:){5}:([0-9a-fA-F]{1,4}:){0,1}[0-9a-fA-F]{1,4}$|" + "^([0-9a-fA-F]{1,4}:){6}:[0-9a-fA-F]{1,4}$");


    public static String getLocalIPv6Addresses() {
        StringBuilder ipv6Addresses = new StringBuilder();
        Map<String, String> interfaceToIpMap = new HashMap<>(); // 存储每个网卡的第一个有效IPv6地址
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // 跳过回环接口和未启用的接口
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet6Address) {
                        Inet6Address inet6Address = (Inet6Address) addr;
                        String ip = inet6Address.getHostAddress();
                        // 移除接口标识符（如 %en0）
                        int scopeIndex = ip.indexOf('%');
                        if (scopeIndex != -1) {
                            ip = ip.substring(0, scopeIndex);
                        }
                        // 跳过本地链接地址、回环地址和包含"0:0:0"的地址
                        if (!inet6Address.isLinkLocalAddress() &&
                                !inet6Address.isSiteLocalAddress() &&
                                !ip.startsWith("fe80:") &&
                                !ip.startsWith("0:0:0:0:0:0:0:1") &&
                                !ip.equals("::1") &&
                                !ip.contains("0:0:0")) {

                            // 如果当前网卡还没有记录，则添加该IP
                            if (!interfaceToIpMap.containsKey(iface.getDisplayName())) {
                                interfaceToIpMap.put(iface.getDisplayName(), "网卡: " + iface.getDisplayName() + " - IPv6: " + ip);
                            }
                        }
                    }
                }
            }

            // 将结果拼接到StringBuilder中
            for (String entry : interfaceToIpMap.values()) {
                ipv6Addresses.append(entry).append("\n");
            }

            if (ipv6Addresses.length() == 0) {
                ipv6Addresses.append("未找到可公网访问的IPv6地址");
            }
        } catch (SocketException e) {
            ipv6Addresses.append("获取网络接口时出错: ").append(e.getMessage());
        }
        return ipv6Addresses.toString();
    }


    public static boolean isValidIPv6(String ip) {
        return IPV6_PATTERN.matcher(ip).matches();
    }


}
