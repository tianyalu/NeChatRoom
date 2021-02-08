package com.sty.ne.chatroom;

import android.app.Activity;

import com.sty.ne.chatroom.connection.PeerConnectionManager;
import com.sty.ne.chatroom.socket.JavaWebSocket;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/5 10:25 PM
 */
public class WebRTCManager {
    private static final String WS_Url = "wss://47.115.6.127/wss";
    private JavaWebSocket webSocket;
    private PeerConnectionManager peerConnectionManager;

    private static final class LazyHolder {
        private static WebRTCManager INSTANCE = new WebRTCManager();
    }

    private WebRTCManager() {

    }

    public static WebRTCManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void connect(Activity activity, String roomId) {
        webSocket = new JavaWebSocket(activity);
        peerConnectionManager = PeerConnectionManager.getInstance();
        webSocket.connect(WS_Url);
    }
}
