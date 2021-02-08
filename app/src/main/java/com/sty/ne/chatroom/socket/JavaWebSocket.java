package com.sty.ne.chatroom.socket;

import android.app.Activity;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.sty.ne.chatroom.ChatRoomActivity;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 实现socket的类
 * @Author: tian
 * @UpdateDate: 2021/2/5 10:20 PM
 */
public class JavaWebSocket {
    private static final String TAG = JavaWebSocket.class.getSimpleName();
    private WebSocketClient mWebSocketClient;
    private Activity activity;

    public JavaWebSocket(Activity activity) {
        this.activity = activity;
    }

    public void connect(String wssAddress) {
        URI uri = null;
        try {
            uri = new URI(wssAddress);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d(TAG, "onOpen");
                ChatRoomActivity.openActivity(activity);
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "onMessage: " + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "onClose code: " + code + ", reason: " + reason + ", remote: " + remote);
            }

            @Override
            public void onError(Exception ex) {
                Log.d(TAG, "onError: " + ex.getMessage());
                ex.printStackTrace();
            }
        };
        if(wssAddress.startsWith("wss")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustManagerTest()}, new SecureRandom());
                SSLSocketFactory factory = null;
                if(sslContext != null) {
                    factory = sslContext.getSocketFactory();
                }
                if(factory != null) {
                    mWebSocketClient.setSocket(factory.createSocket());
                }
            } catch (NoSuchAlgorithmException | IOException | KeyManagementException e) {
                e.printStackTrace();
            }
        }
        mWebSocketClient.connect();
    }

    /**
     * 客户端向服务器发送信息
     * 事件类型：
     * 1 __join
     * 2 __answer
     * 3 __offer
     * 4 __ice_candidate
     * 5 __peer
     */
    public void joinRoom(String roomId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");
        Map<String, String> childMap= new HashMap<>();
        childMap.put("room", roomId);
        map.put("data", childMap);
        JSONObject jsonObject = new JSONObject(map);
        String jsonString = jsonObject.toJSONString();
        mWebSocketClient.send(jsonString);
    }


    //忽略证书
    private class TrustManagerTest implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
