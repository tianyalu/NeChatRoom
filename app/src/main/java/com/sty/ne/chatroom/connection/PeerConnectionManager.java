package com.sty.ne.chatroom.connection;

import android.app.Activity;
import android.content.Context;

import com.sty.ne.chatroom.socket.JavaWebSocket;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 管理p2p通讯的类
 *
 * @Author: tian
 * @UpdateDate: 2021/2/5 10:24 PM
 */
public class PeerConnectionManager {
    private Context mContext;
    private List<PeerConnection> peerConnections;
    private boolean videoEnable;
    private ExecutorService executor;
    private PeerConnectionFactory factory;
    private EglBase eglBase;

    private static final class LazyHolder {
        private static PeerConnectionManager INSTANCE = new PeerConnectionManager();
    }
    private PeerConnectionManager() {
        peerConnections = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();
    }

    public void initContext(Context context, EglBase eglBase) {
        this.mContext = context;
        this.eglBase = eglBase;
    }

    public static PeerConnectionManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     *  PeerConnectionFactory.Options:
     *
     *  static final int ADAPTER_TYPE_UNKNOWN = 0;  //未知
     *  static final int ADAPTER_TYPE_ETHERNET = 1;  //以太网
     *  static final int ADAPTER_TYPE_WIFI = 2;  //WIFI
     *  static final int ADAPTER_TYPE_CELLULAR = 4;  //移动网络
     *  static final int ADAPTER_TYPE_VPN = 8;  //VPN
     *  static final int ADAPTER_TYPE_LOOPBACK = 16;  //回环
     *  static final int ADAPTER_TYPE_ANY = 32;
     *  public int networkIgnoreMask;  //网络忽略
     *  public boolean disableEncryption;  //解码器
     *  public boolean disableNetworkMonitor;  //网络监控
     */

    public void joinToRoom(JavaWebSocket javaWebSocket, boolean isVideoEnable, ArrayList<String> connections, String myId) {
        this.videoEnable = isVideoEnable;
        //PeerConnection
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(factory == null) {
                    factory = createConnectionFactory();
                }
            }
        });
    }

    private PeerConnectionFactory createConnectionFactory() {
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(),
                true, true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        //其它参数设置成默认的
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(mContext)
                .createInitializationOptions());
        return PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(mContext).createAudioDeviceModule())
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }
}
