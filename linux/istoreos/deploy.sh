#!/bin/bash

# 接收部署目录参数，默认为/data
# 命令示例：bash deploy.sh /data
#   /etc/init.d/ipv6-monitor stop      # 停止服务
#   /etc/init.d/ipv6-monitor disable   # 禁用开机自启


DEPLOY_DIR=${1:-"/data"}

# 检查并创建部署目录
if [ ! -d "$DEPLOY_DIR" ]; then
    echo "创建部署目录: $DEPLOY_DIR"
    mkdir -p "$DEPLOY_DIR"
fi

# 检查并安装所需软件包
opkg update

# 检查 msmtp 是否已安装
if ! opkg list-installed | grep -q "^msmtp"; then
    echo "正在安装 msmtp..."
    opkg install msmtp
else
    echo "msmtp 已安装"
fi
# 直接将 ipv6-monitor.sh 脚本写入指定目录，并设置 DEPLOY_DIR 变量
cat > "$DEPLOY_DIR/ipv6-monitor.sh" << EOF
#!/bin/bash
# 此文件放到 $DEPLOY_DIR 目录下

# 部署目录
DEPLOY_DIR="$DEPLOY_DIR"

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
    current_ip=\$(get_current_ipv6)

    # 如果获取不到IP地址，退出
    if [ -z "\$current_ip" ]; then
        echo "无法获取IPv6地址"
        return 1
    fi

    # 检查iprecord文件是否存在
    if [ ! -f "\$DEPLOY_DIR/iprecord" ]; then
        echo "iprecord文件不存在，创建文件并发送通知邮件"
        echo "\$current_ip" > "\$DEPLOY_DIR/iprecord"
        send_notification "\$current_ip" "new"
        return
    fi

    # 读取文件中的IP地址
    recorded_ip=\$(cat "\$DEPLOY_DIR/iprecord" | head -1)

    # 比较IP地址是否相同
    if [ "\$current_ip" != "\$recorded_ip" ]; then
        echo "IP地址发生变化，发送通知邮件"
        echo "\$current_ip" > "\$DEPLOY_DIR/iprecord"
        send_notification "\$current_ip" "changed"
    else
        echo "IP地址未发生变化"
    fi
}

# 发送邮件通知函数
send_notification() {
    local ip_address=\$1
    local status=\$2

    if [ "\$status" == "new" ]; then
        subject="IPv6地址监控通知 - 新IP地址 - \$hostsName"
        body="主机 \$hostsName 检测到新的IPv6地址: \$ip_address \$hostsName"
    else
        subject="IPv6地址监控通知 - IP地址变更 - \$hostsName"
        body="主机 \$hostsName 的IPv6地址已变更: \$ip_address \$hostsName"
    fi

    # 使用msmtp发送邮件
    echo -e "Subject: \$subject\\n\\n\$body" | msmtp 1305133771@qq.com
}

# 无限循环执行检查
while true; do
    check_and_notify
    echo "等待 \${INTERVAL} 秒后再次检查..."
    sleep \$INTERVAL
done
EOF


# 创建服务管理脚本
cat > /etc/init.d/ipv6-monitor << EOF
#!/bin/sh /etc/rc.common

START=99
USE_PROCD=1

SCRIPT_PATH="$DEPLOY_DIR/ipv6-monitor.sh"

start_service() {
    procd_open_instance
    procd_set_param command "\$SCRIPT_PATH"
    procd_set_param respawn
    procd_close_instance
}
EOF

# 添加可执行权限
chmod +x /etc/init.d/ipv6-monitor
chmod +x "$DEPLOY_DIR/ipv6-monitor.sh"

# 启用自启服务
/etc/init.d/ipv6-monitor enable

echo "IPv6监控脚本已部署到 $DEPLOY_DIR 并配置为开机自动启动"
