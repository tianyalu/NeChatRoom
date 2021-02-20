package com.sty.ne.chatroom;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.sty.ne.chatroom.bean.MediaType;
import com.sty.ne.chatroom.bean.MyIceServer;
import com.sty.ne.chatroom.connection.PeerConnectionManager;
import com.sty.ne.chatroom.interfaces.IViewCallback;
import com.sty.ne.chatroom.socket.IConnectEvent;
import com.sty.ne.chatroom.socket.ISignalingEvents;
import com.sty.ne.chatroom.socket.IWebSocket;
import com.sty.ne.chatroom.socket.JavaWebSocket;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制信令和各种操作
 * @Author: tian
 * @UpdateDate: 2021/2/5 10:25 PM
 */
public class WebRTCManager implements ISignalingEvents {
    private static final String TAG = WebRTCManager.class.getSimpleName();
    private String wss;
    private MyIceServer[] iceServers;
    private IConnectEvent iConnectEvent;

    private IWebSocket iwebSocket;
    private PeerConnectionManager peerConnectionManager;

    private String roomId;
    private int mediaType;
    private boolean videoEnable;

    private Handler mHandler;

    private static final class LazyHolder {
        private static WebRTCManager INSTANCE = new WebRTCManager();
    }

    private WebRTCManager() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static WebRTCManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void setViewCallback(IViewCallback iViewCallback) {
        if(peerConnectionManager != null) {
            peerConnectionManager.setViewCallback(iViewCallback);
        }
    }


    public void init(String wss, MyIceServer[] iceServers, IConnectEvent iConnectEvent) {
        this.wss = wss;
        this.iceServers = iceServers;
        this.iConnectEvent = iConnectEvent;
    }

    public void connect(int mediaType, String roomId) {
        if(iwebSocket == null) {
            this.mediaType = mediaType;
            this.videoEnable = mediaType != MediaType.TYPE_AUDIO;
            this.roomId = roomId;
            this.iwebSocket = new JavaWebSocket(this);
            this.iwebSocket.connect(wss);
            this.peerConnectionManager = new PeerConnectionManager(iwebSocket, iceServers);
        }else {
            //正在通话
            iwebSocket.close();
            iwebSocket = null;
            peerConnectionManager = null;
        }
    }

    //========================================↓控制功能↓=============================================
    public void joinRoom(Context context, EglBase eglBase) {
        if(peerConnectionManager != null) {
            peerConnectionManager.initContext(context, eglBase);
        }
        if(iwebSocket != null) {
            iwebSocket.joinRoom(roomId);
        }
    }

    public void switchCamera() {
        if(peerConnectionManager != null) {
            peerConnectionManager.switchCamera();
        }
    }

    public void toggleMute(boolean enableMute) {
        if(peerConnectionManager != null) {
            peerConnectionManager.toggleMute(enableMute);
        }
    }

    public void toggleLarge(boolean enableSpeaker) {
        if(peerConnectionManager != null) {
            peerConnectionManager.toggleLarge(enableSpeaker);
        }
    }

    public void exitRoom() {
        if(peerConnectionManager != null) {
            iwebSocket = null;
            peerConnectionManager.exitRoom();
        }
    }
    //========================================↑控制功能↑=============================================

    //========================================↓信令回调↓=============================================
    @Override
    public void onWebSocketOpen() {
        Log.d(TAG, "onWebSocketOpen()");
        mHandler.post(() -> {
            if(iConnectEvent != null) {
                iConnectEvent.onSuccess();
            }
        });
    }

    @Override
    public void onWebSocketOpenFailed(String msg) {
        Log.d(TAG, "onWebSocketOpenFailed(): " + msg);
        mHandler.post(() -> {
            if(iwebSocket != null && !iwebSocket.isOpen()) {
                if (iConnectEvent != null) {
                    iConnectEvent.onFailed(msg);
                }
            }else if(peerConnectionManager != null){
                peerConnectionManager.exitRoom();
            }
        });
    }

    @Override
    public void onJoinToRoom(ArrayList<String> connections, String myId) {
        Log.d(TAG, "onJoinToRoom() myId: " + myId);
        mHandler.post(() -> {
            if(peerConnectionManager != null) {
                peerConnectionManager.onJoinToRoom(connections, myId, videoEnable, mediaType);
                if (mediaType == MediaType.TYPE_VIDEO || mediaType == MediaType.TYPE_MEETING) {
                    toggleLarge(true);
                }
            }
        });
    }

    @Override
    public void onRemoteJoinToRoom(String socketId) {
        Log.d(TAG, "oonRemoteJoinToRoom() socketId: " + socketId);
        mHandler.post(() -> {
            if(peerConnectionManager != null) {
                peerConnectionManager.onRemoteJoinToRoom(socketId);
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(String socketId, IceCandidate iceCandidate) {
        Log.d(TAG, "onRemoteIceCandidate() socketId: " + socketId);
        mHandler.post(() -> {
            if(peerConnectionManager != null) {
                peerConnectionManager.onRemoteIceCandidate(socketId, iceCandidate);
            }
        });
    }

    @Override
    public void onRemoteIceCandidateRemove(String socketId, List<IceCandidate> iceCandidates) {
        Log.d(TAG, "onRemoteIceCandidateRemove() socketId: " + socketId);
        mHandler.post(() -> {
            if(peerConnectionManager != null) {
                peerConnectionManager.onRemoteIceCandidateRemove(socketId, iceCandidates);
            }
        });
    }

    @Override
    public void onRemoteOutRoom(String socketId) {
        Log.d(TAG, "onRemoteOutRoom() socketId: " + socketId);
        mHandler.post(() -> {
            if(peerConnectionManager != null) {
                peerConnectionManager.onRemoteOutRoom(socketId);
            }
        });
    }

    @Override
    public void onReceiveOffer(String socketId, String sdp) {
        Log.d(TAG, "onReceiveOffer() socketId: " + socketId + " sdp: " + sdp);
        mHandler.post(() -> {
            if(peerConnectionManager != null) {
                peerConnectionManager.onReceiveOffer(socketId, sdp);
            }
        });
    }

    @Override
    public void onReceiverAnswer(String socketId, String sdp) {
        Log.d(TAG, "onReceiverAnswer() socketId: " + socketId + " sdp: " + sdp);
        mHandler.post(() -> {
            if(peerConnectionManager != null) {
                peerConnectionManager.onReceiverAnswer(socketId, sdp);
            }
        });
    }
}
