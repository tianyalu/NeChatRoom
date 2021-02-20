package com.sty.ne.chatroom;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.sty.ne.chatroom.bean.MediaType;
import com.sty.ne.chatroom.bean.MyIceServer;
import com.sty.ne.chatroom.socket.IConnectEvent;
import com.sty.ne.chatroom.socket.JavaWebSocket;
import com.sty.ne.chatroom.ui.ChatRoomActivity;
import com.sty.ne.chatroom.ui.ChatSingleActivity;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/19 3:57 PM
 */
public class WebRTCUtil {
    private static final String TAG = WebRTCUtil.class.getSimpleName();
    private static WebSocketClient mWebSocketClient;
    private static final String WSS = "wss://47.115.6.127/wss";

    private static MyIceServer[] iceServers = {
        new MyIceServer("stun:47.115.6.127:3478?transport=udp"),
        new MyIceServer("turn:47.115.6.127:3478?transport=udp", "tianyalu", "123456")
    };

    //one to one
    public static void callSingle(Activity activity, String wss, String roomId, boolean videoEnable) {
        if(TextUtils.isEmpty(wss)) {
            wss = WSS;
        }
        WebRTCManager.getInstance().init(wss, iceServers, new IConnectEvent() {

            @Override
            public void onSuccess() {
                ChatSingleActivity.openActivity(activity, videoEnable);
            }

            @Override
            public void onFailed(String msg) {

            }
        });
        WebRTCManager.getInstance().connect(videoEnable ? MediaType.TYPE_VIDEO : MediaType.TYPE_AUDIO, roomId);
    }

    //one to one
    public static void call(Activity activity, String wss, String roomId) {
        if(TextUtils.isEmpty(wss)) {
            wss = WSS;
        }
        WebRTCManager.getInstance().init(wss, iceServers, new IConnectEvent() {

            @Override
            public void onSuccess() {
                ChatRoomActivity.openActivity(activity);
            }

            @Override
            public void onFailed(String msg) {
                Log.e(TAG, "WebRTCManager onFailed: " + msg);
            }
        });
        WebRTCManager.getInstance().connect(MediaType.TYPE_MEETING, roomId);
    }

    // test wss
    public static void testWs(String wss) {
        URI uri;
        try {
            uri = new URI(wss);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                Log.e(TAG, "onOpen:");
                mWebSocketClient.send("");
            }

            @Override
            public void onMessage(String message) {
                Log.e(TAG, "onMessage:" + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.e(TAG, "onClose:" + reason);
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "onError:");
                Log.e(TAG, ex.toString());
            }
        };

        if (wss.startsWith("wss")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                if (sslContext != null) {
                    sslContext.init(null, new TrustManager[]{new JavaWebSocket.TrustManagerTest()}, new SecureRandom());
                }

                SSLSocketFactory factory = null;
                if (sslContext != null) {
                    factory = sslContext.getSocketFactory();
                }

                if (factory != null) {
                    mWebSocketClient.setSocket(factory.createSocket());
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mWebSocketClient.connect();
    }
}
