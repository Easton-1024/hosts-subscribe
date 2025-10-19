package com.xyt.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.base.Throwables;
import com.xyt.project.model.IPInfoDTO;
import com.xyt.project.util.AppUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 网络服务
 */
@Slf4j
public class NetworkService {

    private static final String LOCAL_IP_FILE_NAME = "localIP.json";

    /**
     * 获取本机网络地址（增强版）
     * 包含IPv4和IPv6
     * 不包含本地链接地址、回环地址和包含"0:0:0"的地址
     *
     * @return 网卡名称%IP地址
     */
    public List<IPInfoDTO> getLocalNetworkAddressList() {
        List<IPInfoDTO> addressList = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // 跳过无效的网络接口
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                // 获取接口地址列表（包含更多信息）
                List<java.net.InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                for (java.net.InterfaceAddress interfaceAddr : interfaceAddresses) {
                    InetAddress address = interfaceAddr.getAddress();

                    if (address == null) continue;

                    // 获取地址字符串
                    String hostAddress = address.getHostAddress();
                    String displayName = networkInterface.getDisplayName();

                    // 去除IPv6地址后面的作用域ID（%后面的内容）
                    int scopeIndex = hostAddress.indexOf('%');
                    if (scopeIndex > 0) {
                        hostAddress = hostAddress.substring(0, scopeIndex);
                    }

                    // 确定IP地址类型
                    String ipType = address instanceof java.net.Inet6Address ? "IPv6" : "IPv4";

                    // 判断IPv6地址类型（稳定地址或临时地址）
                    String addressType = "";
                    if (address instanceof java.net.Inet6Address) {
                        if (isTemporaryIPv6Address(hostAddress)) {
                            addressType = "(临时地址)";
                        } else {
                            addressType = "(稳定地址)";
                        }
                    }

                    // 过滤条件：
                    // 1. 不是回环地址
                    // 2. 不是本地链接地址
                    // 3. 不包含"0:0:0"
                    if (!address.isLoopbackAddress() &&
                            !hostAddress.contains("0:0:0") &&
                            !hostAddress.startsWith("fe80")) {
                        addressList.add(new IPInfoDTO(displayName, ipType, addressType, hostAddress));
                    }
                }
            }
        } catch (SocketException e) {
            log.error("获取本地网络地址失败: {}" , Throwables.getStackTraceAsString(e));
        }

        return addressList;
    }

    /**
     * 判断IPv6地址是否为临时地址
     * 使用启发式方法判断，基于IPv6地址生成规律
     *
     * @param ipv6Address IPv6地址字符串
     * @return true表示是临时地址，false表示是稳定地址
     */
    private boolean isTemporaryIPv6Address(String ipv6Address) {
        // 临时地址通常具有更高的随机性
        // 稳定地址通常有可识别的模式（如基于MAC地址生成）

        // 检查地址是否以典型的全局单播地址开头
        if (!ipv6Address.startsWith("2")) {
            // 不是以2开头的地址可能不是公网地址
            return false;
        }

        // 尝试通过地址的接口标识符部分判断
        // IPv6地址格式: xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx
        // 最后64位是接口标识符（对于EUI-64生成的稳定地址有特定模式）
        String[] parts = ipv6Address.split(":");
        if (parts.length >= 8) {
            // 获取接口标识符部分（最后4个段）
            String iidPart1 = parts[parts.length - 2]; // 倒数第二个段
            String iidPart2 = parts[parts.length - 1]; // 最后一个段

            // EUI-64生成的稳定地址具有特定模式：
            // 1. 第四组的第2个字符通常是十六进制的 'f' 或 'e'（因为MAC地址转换）
            // 2. 前三个段相对固定（网络部分）

            // 如果最后几段看起来是完全随机的数字，则很可能是临时地址
            // 这是一种启发式判断，不是绝对准确

            // 简单检查：如果最后两段都是纯数字且没有明显的MAC地址特征
            if (iidPart1.matches("[0-9a-f]{4}") && iidPart2.matches("[0-9a-f]{4}")) {
                // 进一步检查是否有EUI-64的特征（第2个字符是'e'或'f'）
                if (iidPart1.length() > 1) {
                    char secondChar = iidPart1.charAt(1);
                    // 如果不是EUI-64的典型特征，则认为是临时地址
                    if (secondChar != 'e' && secondChar != 'f') {
                        return true; // 可能是临时地址
                    }
                }
            }
        }

        // 默认认为是稳定地址
        return false;
    }



    public void saveLocalIP(String filePath) {
        List<IPInfoDTO> ipInfoDTOList = getLocalNetworkAddressList();
        String configJson = JSONUtil.toJsonStr(ipInfoDTOList);
        File file = new File(filePath, LOCAL_IP_FILE_NAME);
        FileUtil.writeUtf8String(configJson, file);
    }

    public List<IPInfoDTO> readLocalIP(String filePath) {
        File file = new File(filePath, LOCAL_IP_FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        String configJson = FileUtil.readUtf8String(file);
        List<IPInfoDTO> ipInfoDTOList = JSONUtil.toList(configJson, IPInfoDTO.class);
        // 使用 Hutool 的 FileUtil.readLines 方法读取文件内容
        return ipInfoDTOList;

    }


public List<IPInfoDTO> filterIPList() {
    List<IPInfoDTO> newIPList = getLocalNetworkAddressList();
    List<IPInfoDTO> oldIpList = readLocalIP(AppUtil.getAppDir());
    if (CollUtil.isEmpty(oldIpList)){
        return newIPList;
    }

    List<IPInfoDTO> changedOrNewIPs = new ArrayList<>();

    // 查找新增或变更的IP地址
    for (IPInfoDTO newIP : newIPList) {
        boolean isNew = true;
        for (IPInfoDTO oldIP : oldIpList) {
            // 如果找到匹配的IP记录，比较其详细信息
            if (newIP.getIpAddress().equals(oldIP.getIpAddress())) {
                isNew = false;
                // 检查其他属性是否有变化
                if (!newIP.getNetworkName().equals(oldIP.getNetworkName()) ||
                    !newIP.getIpType().equals(oldIP.getIpType()) ||
                    !newIP.getAddressType().equals(oldIP.getAddressType())) {
                    // 有变化，添加到结果列表
                    changedOrNewIPs.add(newIP);
                }
                break;
            }
        }
        // 如果是新IP地址，添加到结果列表
        if (isNew) {
            changedOrNewIPs.add(newIP);
        }
    }
    saveLocalIP(AppUtil.getAppDir());
    return changedOrNewIPs;
}



}
