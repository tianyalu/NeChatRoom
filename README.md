# Webrtc聊天室

[TOC]

## 一、理论

### 1.1 参考文档

[java websocket clientssl(wss)](https://blog.csdn.net/vertx/article/details/8469535)

### 1.2 坑

分别先后启动`turnserver`服务和`webrtc`服务：

```bash
/usr/local/bin/turnserver --syslog -a -f --min-port=32355 --max-port=65535 --user=tianyalu:123456 -r test --cert=/cert/cert.pem --pkey==/cert/cert.pem --log-file=stdout -v
```

```bash
cd /root/webrtc/WebrtcNodeJS
node server.js
```

然后点击 加入房间按钮后报错了。

#### 1.2.1 报错1

```bash
W/System.err: java.net.ConnectException: failed to connect to /47.115.6.127 (port 443) from /:: (port 42665): connect failed: ETIMEDOUT (Connection timed out)
```

网络防火墙问题，解决如下：

进入阿里云`ECS`后台，安全组 --> 配置规则，添加`https`的443端口：

![image](https://github.com/tianyalu/NeChatRoom/raw/master/show/webrtc_problem1.png)

#### 1.2.2 报错2

解决报错1后重新点击加入房间按钮，又报错如下：

```bash
java.net.ConnectException: failed to connect to /47.115.6.127 (port 443) from /:: (port 42841): connect failed: ECONNREFUSED (Connection refused)
```

解决方法：要启动`nginx`服务：

```bash
cd /usr/local/nginx/sbin
./nginx
```

启动之后再尝试就能成功建立连接了。