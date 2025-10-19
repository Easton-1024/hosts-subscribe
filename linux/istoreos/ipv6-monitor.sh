#!/bin/bash
# 此文件放到 /data 目录下

# 获取主机名
hostsName="xyt-istoreos.com"

# 设置间隔时间（秒）
INTERVAL=20  # 每60秒执行一次

# 获取当前主机的完整IPv6地址
get_current_ipv6() {
    ip -6 addr show scope global | grep -oE '([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}' | sort -u | head -1
}

# 检查IP变化并发送邮件
check_and_notify() {
    current_ip=$(get_current_ipv6)

    # 如果获取不到IP地址，退出
    if [ -z "$current_ip" ]; then
        echo "无法获取IPv6地址"
        return 1
    fi

    # 检查iprecord文件是否存在
    if [ ! -f "iprecord" ]; then
        echo "iprecord文件不存在，创建文件并发送通知邮件"
        echo "$current_ip" > iprecord
        send_notification "$current_ip" "new"
        return
    fi

    # 读取文件中的IP地址
    recorded_ip=$(cat iprecord | head -1)

    # 比较IP地址是否相同
    if [ "$current_ip" != "$recorded_ip" ]; then
        echo "IP地址发生变化，发送通知邮件"
        echo "$current_ip" > iprecord
        send_notification "$current_ip" "changed"
    else
        echo "IP地址未发生变化"
    fi
}

# 发送邮件通知函数
send_notification() {
    local ip_address=$1
    local status=$2

    if [ "$status" == "new" ]; then
        subject="IPv6地址监控通知 - 新IP地址 - $hostsName"
        body="主机 $hostsName 检测到新的IPv6地址: $ip_address $hostsName"
    else
        subject="IPv6地址监控通知 - IP地址变更 - $hostsName"
        body="主机 $hostsName 的IPv6地址已变更: $ip_address $hostsName"
    fi

    # 使用msmtp发送邮件
    echo -e "Subject: $subject\n\n$body" | msmtp 1305133771@qq.com
}

# 无限循环执行检查
while true; do
    check_and_notify
    echo "等待 ${INTERVAL} 秒后再次检查..."
    sleep $INTERVAL
done
