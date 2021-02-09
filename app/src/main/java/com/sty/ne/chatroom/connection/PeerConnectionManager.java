package com.sty.ne.chatroom.connection;

import android.content.Context;

import com.sty.ne.chatroom.ChatRoomActivity;
import com.sty.ne.chatroom.socket.JavaWebSocket;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
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
    private AudioTrack localAudioTrack; //音轨
    private VideoCapturer captureAndroid; //获取摄像头设备：camera1 camera2 前置还是后置
    private VideoSource videoSource; //视频源
    private SurfaceTextureHelper surfaceTextureHelper; //帮助渲染到本地预览
    private VideoTrack localVideoTrack; //视频轨

    private String myId;

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
        this.myId = myId;
        //PeerConnection
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(factory == null) {
                    factory = createConnectionFactory();
                }

                if(localStream == null) {
                    createLocalStream();
                }
            }
        });
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
}
