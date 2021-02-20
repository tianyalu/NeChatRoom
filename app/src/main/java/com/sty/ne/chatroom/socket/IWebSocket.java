package com.sty.ne.chatroom.socket;

import org.webrtc.IceCandidate;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/19 4:17 PM
 */
public interface IWebSocket {

    void connect(String wss);

    boolean isOpen();

    void close();

    //加入房间
    void joinRoom(String room);

    //处理回调消息
    void handleMessage(String message);


    void sendIceCandidate(String socketId, IceCandidate iceCandidate);

    void sendAnswer(String socketId, String sdp);

    void sendOffer(String socketId, String sdp);
}
