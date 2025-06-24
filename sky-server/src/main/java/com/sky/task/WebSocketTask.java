package com.sky.task;


import com.sky.webSocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * @author wyr on 2025/6/24
 */
@Component
public class WebSocketTask {
    @Autowired
    private WebSocketServer webSocketServer;
    @Scheduled(cron = "0/5 * * * * ?") // 每5秒执行一次
    public void sendMessage() {
        webSocketServer.sendToAllClient("这是来自服务端的消息：" + DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalTime.now()));
    }
}
