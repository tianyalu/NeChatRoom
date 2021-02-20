package com.sty.ne.chatroom.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.sty.ne.chatroom.ProxyVideoSink;
import com.sty.ne.chatroom.R;
import com.sty.ne.chatroom.WebRTCManager;
import com.sty.ne.chatroom.interfaces.IViewCallback;
import com.sty.ne.chatroom.utils.PermissionUtil;
import com.sty.ne.chatroom.utils.ScreenUtils;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

/**
 * 群聊界面
 * @Author: tian
 * @UpdateDate: 2021/2/8 4:07 PM
 */
public class ChatRoomActivity extends AppCompatActivity implements IViewCallback {
    private static final String TAG = ChatRoomActivity.class.getSimpleName();
    private FrameLayout flVideoLayout;
    private WebRTCManager webRTCManager;
    private EglBase rootEglBase;
    private VideoTrack localVideoTrack;

    private Map<String, SurfaceViewRenderer> videoViews = new HashMap<>();
    private Map<String, ProxyVideoSink> sinks = new HashMap<>();
    private List<String> persons = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        initView();
        startCall();
    }

    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        activity.startActivity(intent);
    }


    private void initView() {
        rootEglBase = EglBase.create();
        flVideoLayout = findViewById(R.id.fl_video_view);
        flVideoLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ChatRoomFragment chatRoomFragment = new ChatRoomFragment();
        replaceFragment(chatRoomFragment);
    }

    private void replaceFragment(ChatRoomFragment chatRoomFragment) {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fl_container, chatRoomFragment)
                .commit();
    }

    private void startCall() {
        webRTCManager = WebRTCManager.getInstance();
        webRTCManager.setViewCallback(this);
        if(!PermissionUtil.isNeedRequestPermission(this)) {
            webRTCManager.joinRoom(this, rootEglBase);
        }
    }

    /**
     *
     * @param stream 本地流
     * @param userId 自己的ID
     */
    @Override
    public void onSetLocalStream(MediaStream stream, String userId) {
        List<VideoTrack> videoTracks = stream.videoTracks;
        if(videoTracks.size() > 0) {
            localVideoTrack = videoTracks.get(0);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(userId, stream);
            }
        });
    }

    /**
     *
     * @param userId 用户ID
     * @param stream 视频流（本地流或者远端的视频流）
     */
    private void addView(String userId, MediaStream stream) {
        //不使用SurfaceView 而是采用webrtc给我们提供的 SurfaceViewRenderer
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(ChatRoomActivity.this);
        //初始化SurfaceView
        renderer.init(rootEglBase.getEglBaseContext(), null);
        //设置缩放模式：SCALE_ASPECT_FIT 按照view的宽度和高度设置；SCALE_ASPECT_FILL 按照摄像头预览的画面大小设置
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        //翻转
        renderer.setMirror(true);
        // set render
        ProxyVideoSink sink = new ProxyVideoSink();
        sink.setTarget(renderer);
        //将摄像头的数据渲染到surfaceViewRender
        if(stream.videoTracks.size() > 0) {
            stream.videoTracks.get(0).addSink(sink);
        }

        //会议室 1+N 个人
        videoViews.put(userId, renderer);
        sinks.put(userId, sink);
        persons.add(userId);

        //将SurfaceViewRenderer添加到FrameLayout中（此时width=0, height=0）
        flVideoLayout.addView(renderer);
        //指定宽度和高度
        int size = videoViews.size();
        Log.d(TAG, "size: " + size);
        for (int i = 0; i < size; i++) {
            String peerId = persons.get(i);
            SurfaceViewRenderer renderer1 = videoViews.get(peerId);
            if(renderer1 != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.width = ScreenUtils.getWidth(size);
                layoutParams.height = ScreenUtils.getWidth(size);
                layoutParams.leftMargin = ScreenUtils.getX(size, i);
                layoutParams.topMargin = ScreenUtils.getY(size, i);
                Log.d(TAG, "width: " + layoutParams.width + " height: " + layoutParams.height +
                        " leftMargin: " + layoutParams.leftMargin + " topMargin: " + layoutParams.topMargin);
                renderer1.setLayoutParams(layoutParams);
            }
        }
    }

    @Override
    public void onAddRemoteStream(MediaStream mediaStream, String socketId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(socketId, mediaStream);
            }
        });

    }

    //静音
    public void toggleMute(boolean enableMute) {
        webRTCManager.toggleMute(enableMute);
    }

    //免提
    public void toggleLarge(boolean enableSpeaker) {
        webRTCManager.toggleLarge(enableSpeaker);
    }

    public void toggleCamera(boolean enableCamera) {
        if(localVideoTrack != null) {
            //是否关闭摄像头预览
            localVideoTrack.setEnabled(enableCamera);
        }
    }

    public void switchCamera() {
        webRTCManager.switchCamera();
    }

    public void hangUp() {
        exit();
        this.finish();
    }

    @Override
    public void onCloseWithId(String id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeView(id);
            }
        });
    }

    private void removeView(String id) {
        //找到会议室对应的人 布局
        ProxyVideoSink sink = sinks.get(id);
        if(sink != null) {
            sink.setTarget(null);
        }
        sinks.remove(id);

        SurfaceViewRenderer renderer = videoViews.get(id);
        if(renderer != null) {
            //释放surfaceView
            renderer.release();
            videoViews.remove(id);
            persons.remove(id);
            //父容器移除surfaceView
            flVideoLayout.removeView(renderer);

            int size = videoViews.size();
            for (int i = 0; i < size; i++) {
                String peerId = persons.get(i);
                SurfaceViewRenderer renderer1 = videoViews.get(peerId);
                if(renderer1 != null) {
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    layoutParams.width = ScreenUtils.getWidth(size);
                    layoutParams.height = ScreenUtils.getWidth(size);
                    layoutParams.leftMargin = ScreenUtils.getX(size, i);
                    layoutParams.topMargin = ScreenUtils.getY(size, i);
                    renderer1.setLayoutParams(layoutParams);
                }
            }
        }
    }

    //屏蔽返回键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        exit();
        super.onDestroy();
    }

    private void exit() {
        webRTCManager.exitRoom();
        for (SurfaceViewRenderer renderer : videoViews.values()) {
            renderer.release();
        }
        for (ProxyVideoSink sink : sinks.values()) {
            sink.setTarget(null);
        }
        videoViews.clear();
        sinks.clear();
        persons.clear();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            Log.i(TAG, "[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }
        webRTCManager.joinRoom(this, rootEglBase);
    }
}
