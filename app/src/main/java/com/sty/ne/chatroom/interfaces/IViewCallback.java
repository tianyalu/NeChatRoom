package com.sty.ne.chatroom.interfaces;

import org.webrtc.MediaStream;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/10 4:58 PM
 */
public interface IViewCallback {
    void onSetLocalStream(MediaStream stream, String socketId);
    void onAddRemoteStream(MediaStream stream, String socketId);
    void onCloseWithId(String socketId);
}
