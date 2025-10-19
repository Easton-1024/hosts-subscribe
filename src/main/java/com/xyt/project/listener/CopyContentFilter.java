package com.xyt.project.listener;

/**
 * 复制内容过滤器
 */
public class CopyContentFilter {


    public String filtration(String displayText) {
        StringBuilder ipv6Addresses = new StringBuilder();
        String[] lines = displayText.split("\n");

        for (String line : lines) {
            // 从类似 "网卡: en0 - IPv6: 2001:db8::1" 的文本中提取IPv6地址
            int ipv6Index = line.indexOf("IPv6: ");
            if (ipv6Index != -1) {
                String ipv6 = line.substring(ipv6Index + 6).trim(); // 6是"IPv6: "的长度
                // 移除可能存在的额外字符（虽然现在应该已经没有了）
                int scopeIndex = ipv6.indexOf('%');
                if (scopeIndex != -1) {
                    ipv6 = ipv6.substring(0, scopeIndex);
                }
                // 过滤掉包含"0:0:0"的地址
                if (!ipv6.contains("0:0:0")) {
                    if (ipv6Addresses.length() > 0) {
                        ipv6Addresses.append("\n");
                    }
                    ipv6Addresses.append(ipv6);
                }
            }
        }

        return ipv6Addresses.toString();
    }

}
