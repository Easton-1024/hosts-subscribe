package com.xyt.project.model;

public class IPInfoDTO {

    /**
     * 网卡名称
     */
    private String networkName;

    /**
     * ip类型 IPv4/IPv6
     */
    private String ipType;

    /**
     * ip地址类型
     */
    private String addressType;

    /**
     * ip地址
     */
    private String ipAddress;

    public IPInfoDTO() {
    }

    public IPInfoDTO(String networkName, String ipType, String addressType, String ipAddress) {
        this.addressType = addressType;
        this.ipAddress = ipAddress;
        this.ipType = ipType;
        this.networkName = networkName;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getIpType() {
        return ipType;
    }

    public void setIpType(String ipType) {
        this.ipType = ipType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }
}
