package com.sty.ne.chatroom.socket;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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
public class JavaWebSocket implements IWebSocket {
    private static final String TAG = JavaWebSocket.class.getSimpleName();
    private WebSocketClient mWebSocketClient;
    private ISignalingEvents events;
    private boolean isOpen; //是否连接成功过

    public JavaWebSocket(ISignalingEvents events) {
        this.events = events;
    }

    @Override
    public void connect(String wssAddress) {
        URI uri;
        try {
            uri = new URI(wssAddress);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        if(mWebSocketClient == null) {
            mWebSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    Log.d(TAG, "onOpen");
                    isOpen = true;
                    if(events != null) {
                        events.onWebSocketOpen();
                    }
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "onMessage: " + message);
                    isOpen = true;
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "onClose code: " + code + ", reason: " + reason + ", remote: " + remote);
                    isOpen = false;
                    if(events != null) {
                        events.onWebSocketOpenFailed(reason);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Log.d(TAG, "onError: " + ex.getMessage());
                    ex.printStackTrace();
                    isOpen = false;
                    if(events != null) {
                        events.onWebSocketOpenFailed(ex.toString());
                    }
                }
            };
        }
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

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() {
        if(mWebSocketClient != null) {
            mWebSocketClient.close();
        }
    }

    //==================================↓需要发送的↓===================================
    /**
     * 客户端向服务器发送信息
     * 事件类型：
     * 1 __join
     * 2 __answer
     * 3 __offer
     * 4 __ice_candidate
     * 5 __peer
     *
     * {"data":{"room":"666555"},"eventName":"__join"}
     */
    @Override
    public void joinRoom(String roomId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");
        Map<String, String> childMap= new HashMap<>();
        childMap.put("room", roomId);
        map.put("data", childMap);
        JSONObject jsonObject = new JSONObject(map);
        String jsonString = jsonObject.toJSONString();
        Log.d(TAG, "joinRoom--> " + jsonString);
        mWebSocketClient.send(jsonString);
    }

    @Override
    public void sendAnswer(String socketId, String sdp) {
        Map<String, Object> childMap1 = new HashMap<>();
        childMap1.put("type", "answer");
        childMap1.put("sdp", sdp);
        HashMap<String, Object> childMap2 = new HashMap<>();
        childMap2.put("socketId", socketId);
        childMap2.put("sdp", childMap1);
        HashMap<String, Object> map = new HashMap<>();
        map.put("eventName", "__answer");
        map.put("data", childMap2);
        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "sendAnswer--> " + jsonString);
        mWebSocketClient.send(jsonString);
    }

    /**
     * 数据格式：
     * {
     *     "data": {
     *         "socketId": "xxx-id-xxx",
     *         "sdp" : {
     *              "type": "answer",
     *              "sdp": "v=0\r\nno=-60925xxxxx"
     *         }
     *     },
     *     "eventName": "_answer"
     * }
     *
     * 只在交换的时候传递一次
     *
     * @param socketId
     * @param sdp
     */
    @Override
    public void sendOffer(String socketId, String sdp) {
        HashMap<String, Object> childMap = new HashMap<>();
        childMap.put("type", "offer");
        childMap.put("sdp", sdp);

        HashMap<String, Object> childMap2 = new HashMap<>();
        childMap2.put("socketId", socketId);
        childMap2.put("sdp", childMap);

        HashMap<String, Object> map = new HashMap<>();
        map.put("eventName", "__offer");
        map.put("data", childMap2);

        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "sendOffer -> " + jsonString);
        mWebSocketClient.send(jsonString);
    }

    /**
     * 数据格式：
     * {
     *     "eventName": "_ice_candidate",
     *     "data": {
     *         "id": "audio",
     *         "label": 0,
     *         "candidate":  "candidate:559267639 1 udp 2122202367 ::1 43353 host generation 0 ufrag sKV4 network-id 2",
     *         "socketId": "xxx-id-xxx"
     *     }
     * }
     *
     * 可以传递多次
     *
     * @param socketId
     * @param iceCandidate
     */
    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {
        HashMap<String, Object> childMap = new HashMap<>();
        childMap.put("id", iceCandidate.sdpMid);
        childMap.put("label", iceCandidate.sdpMLineIndex);
        childMap.put("candidate", iceCandidate.sdp);
        childMap.put("socketId", socketId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("eventName", "__ice_candidate");
        map.put("data", childMap);

        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "sendIceCandidate --> " + jsonString);
        mWebSocketClient.send(jsonString);
    }
    //==================================↑需要发送的↑===================================


    //==================================↓需要接收的↓===================================
    /**
     * send {"data": {"room": "666555"}, "eventName": "__join"}
     * onMessage: {"eventName": "_peers", "data": {"connections": ["xxx-id1-xxx", "xxx-id2-xxx"], "you": "xxx-id0-xxx"}}
     * onMessage: {"eventName": "_new_peer", "data": {"socketId": "xxx-idx-xxx"}}
     * @param message
     */
    @Override
    public void handleMessage(String message) {
        Map map = JSON.parseObject(message, Map.class);
        String eventName = (String) map.get("eventName");
        if(TextUtils.isEmpty(eventName)) {
            return;
        }
        //p2p通信
        if("_peers".equals(eventName)) {
            handleJoinToRoom(map);
        }
        if("_new_peer".equals(eventName)) {
            handleRemoteInRoom(map);
        }
        // offer 主叫 对方响应 _ice_candidate  对方的小目标--> 大目标json
        if("_ice_candidate".equals(eventName)) {
            handleRemoteCandidate(map);
        }
        if("_remove_peer".equals(eventName)) {
            handleRemoteOutRoom(map);
        }
        if("_offer".equals(eventName)) {
            handleOffer(map);
        }
        // 对方的sdp
        if("_answer".equals(eventName)) {
            handleAnswer(map);
        }
    }

    /**
     * 自己进入房间
     * {"eventName":"_peers","data":{"connections":[],"you":"ef6d6df4-5bc4-4f16-b219-1d9ef7ba6b08"}}
     * @param map
     */
    private void handleJoinToRoom(Map map) {
        Log.d(TAG, "JavaWebSocket handleJoinRoom: ");
        Map data = (Map) map.get("data");
        JSONArray arr;
        if(data != null) {
            arr = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(js, String.class);
            String myId = (String) data.get("you");
            if(events != null) {
                //peerConnectionManager.joinToRoom(this, true, connections, myId);
                events.onJoinToRoom(connections, myId);
            }
        }
    }

    //自己已经在房间，有其他人进来
    private void handleRemoteInRoom(Map map) {
        Log.d(TAG, "JavaWebSocket handleRemoteInRoom: ");
        Map data = (Map) map.get("data");
        String socketId;
        if(data != null && events != null) {
            socketId = (String) data.get("socketId");
            events.onRemoteJoinToRoom(socketId);
        }
    }

    /**
     * 处理交换信息
     * 数据格式：和 {@link #sendIceCandidate(String, IceCandidate)} 一致
     * {
     *     "eventName": "_ice_candidate",
     *     "data": {
     *         "id": "audio",
     *         "label": 0,
     *         "candidate":  "candidate:559267639 1 udp 2122202367 ::1 43353 host generation 0 ufrag sKV4 network-id 2",
     *         "socketId": "xxx-id-xxx"
     *     }
     * }
     *
     * @param map
     */
    private void handleRemoteCandidate(Map map) {
        Log.d(TAG, "JavaWebSocket handleRemoteCandidate: ");
        Map data = (Map) map.get("data");
        String socketId;
        if(data != null) {
            socketId = String.valueOf(data.get("socketId"));
            String sdpMid = String.valueOf(data.get("id"));
            sdpMid = (null == sdpMid) ? "video" : sdpMid;
            int sdpMLineIndex = (int) Double.parseDouble(String.valueOf(data.get("label")));
            String candidate = String.valueOf(data.get("candidate"));
            //IceCandidate对象
            IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            if(events != null) {
                events.onRemoteIceCandidate(socketId, iceCandidate);
            }
        }
    }

    //有人离开了房间
    private void handleRemoteOutRoom(Map map) {
        Log.d(TAG, "JavaWebSocket handleRemoteOutRoom: ");
        Map data = (Map) map.get("data");
        String socketId;
        if(data != null && events != null) {
            socketId = (String) data.get("socketId");
            events.onRemoteOutRoom(socketId);
        }
    }

    //处理offer
    private void handleOffer(Map map) {
        Log.d(TAG, "JavaWebSocket handleOffer: ");
        Map data = (Map) map.get("data");
        Map sdpDic;
        if(data != null && events != null) {
            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) sdpDic.get("sdp");
            events.onReceiveOffer(socketId, sdp);
        }
    }

    /**
     * 处理Answer
     * 数据格式：和 {@link #sendOffer(String, String)} 一致
     * {
     *     "data": {
     *         "socketId": "xxx-id-xxx",
     *         "sdp" : {
     *              "type": "answer",
     *              "sdp": "v=0\r\nno=-60925xxxxx"
     *         }
     *     },
     *     "eventName": "_answer"
     * }
     *
     * @param map
     */
    private void handleAnswer(Map map) {
        Log.d(TAG, "JavaWebSocket handleAnswer: ");
        Map data = (Map) map.get("data");
        Map sdpDic;
        if(data != null && events != null) {
            sdpDic = (Map) data.get("sdp");
            String socketId = String.valueOf(data.get("socketId"));
            String sdp = String.valueOf(sdpDic.get("sdp"));
            events.onReceiverAnswer(socketId, sdp);
        }
    }
    //==================================↑需要接收的↑===================================


    //忽略证书
    public static class TrustManagerTest implements X509TrustManager {

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
