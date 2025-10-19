package com.xyt.project.validator;

import java.util.regex.Pattern;

public class IPv6Validator {


    private static final Pattern IPV6_PATTERN = Pattern.compile("..."); // 原有的IPv6正则表达式

    public static boolean isValidIPv6(String ip) {
        // 从 HostsEditor 中提取 isValidIPv6 方法
        return IPV6_PATTERN.matcher(ip).matches();
    }

}
