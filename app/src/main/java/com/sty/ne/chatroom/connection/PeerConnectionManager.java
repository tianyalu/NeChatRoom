package com.sty.ne.chatroom.connection;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.sty.ne.chatroom.ChatRoomActivity;
import com.sty.ne.chatroom.socket.JavaWebSocket;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 管理p2p通讯的类
 *
 * @Author: tian
 * @UpdateDate: 2021/2/5 10:24 PM
 */
public class PeerConnectionManager {
    private static final String TAG = PeerConnectionManager.class.getSimpleName();
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"; //回音消除
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"; //噪声抑制
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"; //自动增益控制
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"; //高通滤波器
    private Context mContext;
    private List<PeerConnection> peerConnections;
    private boolean videoEnable;
    private ExecutorService executor;
    private PeerConnectionFactory factory;
    private EglBase rootEglBase;

    private MediaStream localStream;
    private AudioSource audioSource;
    //音轨
    private AudioTrack localAudioTrack;
    //获取摄像头设备：camera1 camera2 前置还是后置
    private VideoCapturer captureAndroid;
    //视频源
    private VideoSource videoSource;
    //帮助渲染到本地预览
    private SurfaceTextureHelper surfaceTextureHelper;
    //视频轨
    private VideoTrack localVideoTrack;

    private String myId;
    //ICE服务器的集合
    private ArrayList<PeerConnection.IceServer> iceServers;
    //会议室所有用户的ID
    private ArrayList<String> connectionIdArray;
    //会议室的每一个用户会对本地实现一个p2p连接Peer(PeerConnection)
    private Map<String, Peer> connectionPeerDic;
    //当前客户端的角色
    private Role role;

    private JavaWebSocket webSocket;
    //声音服务类
    private AudioManager mAudioManager;

    // 角色：邀请者，被邀请者
    // 1v1: 别人给你音视频通话，你就是Receiver
    // 会议室通话：第一次进入会议室-->Caller，当你已经进入了会议室，别人进入会议室时-->Receiver
    enum Role {Caller, Receiver}



    private static final class LazyHolder {
        private static PeerConnectionManager INSTANCE = new PeerConnectionManager();
    }

    private PeerConnectionManager() {
        peerConnections = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();
    }

    public void initContext(Context context, EglBase eglBase) {
        this.mContext = context;
        this.rootEglBase = eglBase;
        this.iceServers = new ArrayList<>();
        this.connectionPeerDic = new HashMap<>();
        this.connectionIdArray = new ArrayList<>();
        //https
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:47.115.6.127:3478?transport=udp")
                .setUsername("")
                .setPassword("")
                .createIceServer();
        //http
        PeerConnection.IceServer iceServer2 = PeerConnection.IceServer.builder("turn:47.115.6.127:3478?transport=udp")
                .setUsername("tianyalu")
                .setPassword("123456")
                .createIceServer();
        iceServers.add(iceServer);
        iceServers.add(iceServer2);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
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
        this.webSocket = javaWebSocket;
        this.videoEnable = isVideoEnable;
        this.myId = myId;
        //PeerConnection 情况1：会议室以及有人的情况
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(factory == null) {
                    factory = createConnectionFactory();
                }
                if(localStream == null) {
                    createLocalStream();
                }
                connectionIdArray.addAll(connections);
                createPeerConnections();
                //把本地的数据流推向会议室的每一个人的能力
                addStreams();
                //发送邀请
                createOffers();
            }
        });
    }

    //为所有连接添加推流
    private void addStreams() {
        Log.d(TAG, "为所有连接添加流");
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()) {
            if(localStream == null) {
                createLocalStream();
            }
            entry.getValue().peerConnection.addStream(localStream);
        }
    }

    /**
     * 为所有的连接创建offer
     */
    private void createOffers() {
        //邀请
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()) {
            //赋值角色信息
            role = Role.Caller;

            Peer mPeer = entry.getValue();
            //向每一位会议室的人发送邀请，并且传递自己的数据类型（音频、视频的选择）
            mPeer.peerConnection.createOffer(mPeer, offerOrAnswerConstraint());  //内部网路请求
        }
    }

    /**
     * 情况一： 当别人在会议室，我再进去
     * 情况二：
     * @param socketId
     * @param iceCandidate
     */
    public void onRemoteIceCandidate(String socketId, IceCandidate iceCandidate) {
        //通过socketId 取出连接对象
        Peer peer = connectionPeerDic.get(socketId);
        if(peer != null) {
            peer.peerConnection.addIceCandidate(iceCandidate);
        }
    }


    public void onReceiverAnswer(String socketId, String sdp) {
        //对方的回话 sdp
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Peer mPeer = connectionPeerDic.get(socketId);
                SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
                if(mPeer != null) {
                    mPeer.peerConnection.setRemoteDescription(mPeer, sessionDescription);
                }
            }
        });

    }

    public void toggleSpeaker(boolean enableMic) {
        if(localAudioTrack != null) {
            //切换是否允许将本地的麦克风数据推送到远端
            localAudioTrack.setEnabled(enableMic);
        }
    }

    //免提
    public void toggleLarge(boolean enableSpeaker) {
        if(mAudioManager != null) {
            mAudioManager.setSpeakerphoneOn(enableSpeaker);
        }
    }

    //切换前置、后置摄像头
    public void switchCamera() {
        if(captureAndroid == null) {
            return;
        }
        if(captureAndroid instanceof CameraVideoCapturer) {
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) captureAndroid;
            cameraVideoCapturer.switchCamera(null);
        }
    }


    /**
     * 耗时操作 webrtc关闭网络
     */
    public void exitRoom() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> myCopy = (ArrayList<String>) connectionIdArray.clone();
                for (String id : myCopy) {
                    closePeerConnection(id);
                }
                //清空集合
                if(connectionIdArray != null) {
                    connectionIdArray.clear();
                }
                //关闭音频推流
                if(audioSource != null) {
                    audioSource.dispose();
                    audioSource = null;
                }
                //关闭视频推流
                if(videoSource != null) {
                    videoSource.dispose();
                    videoSource = null;
                }
                //关闭摄像头预览
                if(captureAndroid != null) {
                    try {
                        captureAndroid.stopCapture();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //关闭 surfaceTextureHelper 辅助类
                if(surfaceTextureHelper != null) {
                    surfaceTextureHelper.dispose();
                    surfaceTextureHelper = null;
                }
                //关闭工厂
                if(factory != null) {
                    factory.dispose();
                    factory = null;
                }
            }
        });
    }

    private void closePeerConnection(String id) {
        //拿到链接的封装对象
        Peer mPeer = connectionPeerDic.get(id);
        if(mPeer != null) {
            //关闭p2p链接
            mPeer.peerConnection.close();
        }
        connectionPeerDic.remove(id);
        ((ChatRoomActivity) mContext).onCloseWithId(id);
    }


    /**
     * 设置是否传输音视频
     * 音频：
     * 视频：false
     * @return
     */
    private MediaConstraints offerOrAnswerConstraint() {
        //媒体约束
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        //音频必须传输
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        //videoEnable
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", String.valueOf(videoEnable)));

        return mediaConstraints;
    }

    /**
     * 建立对会议室每一个用户的连接
     */
    private void createPeerConnections() {
        for (String id : connectionIdArray) {
            Peer peer = new Peer(id);
            connectionPeerDic.put(id, peer);
        }
    }

    private PeerConnectionFactory createConnectionFactory() {
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(),
                true, true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
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

    private void createLocalStream() {
        localStream = factory.createLocalMediaStream("ARDAMS");
        //音频
        audioSource = factory.createAudioSource(createAudioConstraints());
        //采集音频
        localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);
        localStream.addTrack(localAudioTrack); //添加音轨
        if(videoEnable) {
            //视频源
            captureAndroid = createVideoCapture();
            videoSource = factory.createVideoSource(captureAndroid.isScreencast());
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
            //初始化captureAndroid
            captureAndroid.initialize(surfaceTextureHelper, mContext, videoSource.getCapturerObserver());
            //摄像头预览的宽度、高度和帧率
            captureAndroid.startCapture(320, 240, 10);
            //视频轨
            localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
            localStream.addTrack(localVideoTrack);
            if(mContext != null) {
                ((ChatRoomActivity)mContext).onSetLocalStream(localStream, myId);
            }
        }
    }

    private VideoCapturer createVideoCapture() {
        VideoCapturer videoCapturer = null;
        if(Camera2Enumerator.isSupported(mContext)) {
            Camera2Enumerator enumerator = new Camera2Enumerator(mContext);
            videoCapturer = createCameraCapture(enumerator);
        }else {
            Camera1Enumerator enumerator = new Camera1Enumerator(true);
            videoCapturer = createCameraCapture(enumerator);
        }

        return videoCapturer;
    }

    private VideoCapturer createCameraCapture(CameraEnumerator enumerator) {
        //0:back 1:front
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if(enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if(videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        for (String deviceName : deviceNames) {
            if(!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if(videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private MediaConstraints createAudioConstraints() {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
        return audioConstraints;
    }

    private class Peer implements SdpObserver, PeerConnection.Observer{
        //myId 自己跟远端用户之间的连接
        private PeerConnection peerConnection;
        //socket是其它用户的id
        private String socketId;

        public Peer(String socketId) {
            this.socketId = socketId;
            PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
            peerConnection = factory.createPeerConnection(rtcConfiguration, this);
        }

        //内网状态发生改变，如音视频通话中 4G --> 切换成WiFi
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "onSignalingChange signalingState: " + signalingState);
        }

        //连接上了ICE服务器
        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "onIceConnectionChange iceConnectionState: " + iceConnectionState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "onIceConnectionReceivingChange b: " + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "onIceGatheringChange iceGatheringState: " + iceGatheringState);
        }

        //该方法调用的时机有两类，第一类是在连接到ICE服务器的时候，调用次数是网络中有多少个路由节点（1-n）
        //第二类（有人进入这个房间）对方到ICE服务器的路由节点，调用次数是视频通话的人在网络中离ICE服务器有多少个路由节点（1-n）
        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "onIceCandidate iceCandidate: " + iceCandidate.toString());
            //socket --> 传递
            webSocket.sendIceCandidate(socketId, iceCandidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d(TAG, "onIceCandidatesRemoved iceCandidates: " + Arrays.toString(iceCandidates));
        }

        //p2p建立成功之后，mediaStream(视频流，音频流）
        //子线程
        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "onAddStream mediaStream: " + mediaStream.toString());
            ((ChatRoomActivity)mContext).onAddRemoteStream(mediaStream, socketId);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "onRemoveStream mediaStream: " + mediaStream.toString());
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "onDataChannel dataChannel: " + dataChannel.toString());
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded " );
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d(TAG, "onAddTrack rtpReceiver: " + rtpReceiver.toString()
                    + " \nmediaStreams: " + Arrays.toString(mediaStreams));
        }

        // ---------------------------SDPObserver--------------------------------
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d(TAG, "onCreateSuccess sessionDescription: " + sessionDescription.toString());
            //设置本地的SDP，如果设置成功则回调onSetSuccess()方法
            peerConnection.setLocalDescription(this, sessionDescription);
        }

        @Override
        public void onSetSuccess() {
            Log.d(TAG, "onSetSuccess " );
            //交换彼此的sdp iceCandidate
            if(peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
                //webSocket
                webSocket.sendOffer(socketId, peerConnection.getLocalDescription());
            }
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d(TAG, "onCreateFailure s: " + s );
        }

        @Override
        public void onSetFailure(String s) {
            Log.d(TAG, "onSetFailure s: " + s );
        }
    }
}
