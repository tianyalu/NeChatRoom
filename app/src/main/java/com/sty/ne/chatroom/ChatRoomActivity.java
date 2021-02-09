package com.sty.ne.chatroom;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

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

import androidx.annotation.Nullable;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/8 4:07 PM
 */
public class ChatRoomActivity extends Activity {
    private FrameLayout frVideoLayout;
    private WebRTCManager webRTCManager;
    private EglBase rootEglBase;
    private VideoTrack localVideoTrack;

    private Map<String, SurfaceViewRenderer> videoViews = new HashMap<>();
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
    }

    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        activity.startActivity(intent);
    }


    private void initView() {
        rootEglBase = EglBase.create();
        frVideoLayout = findViewById(R.id.fr_video_view);
        frVideoLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        webRTCManager = WebRTCManager.getInstance();
        if(!PermissionUtil.isNeedRequestPermission(this)) {
            webRTCManager.joinRoom(this, rootEglBase);
        }
    }

    /**
     *
     * @param stream 本地流
     * @param userId 自己的ID
     */
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
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(this);
        //初始化SurfaceView
        renderer.init(rootEglBase.getEglBaseContext(), null);
        //设置缩放模式：SCALE_ASPECT_FIT 按照view的宽度和高度设置；SCALE_ASPECT_FILL 按照摄像头预览的画面大小设置
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        //翻转
        renderer.setMirror(true);
        //将摄像头的数据渲染到surfaceViewRender
        if(stream.videoTracks.size() > 0) {
            stream.videoTracks.get(0).addSink(renderer);
        }

        //会议室 1+N 个人
        videoViews.put(userId, renderer);
        persons.add(userId);

        //将SurfaceViewRenderer添加到FrameLayout中（此时width=0, height=0）
        frVideoLayout.addView(renderer);
        //指定宽度和高度
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
                layoutParams.topMargin = ScreenUtils.getX(size, i);
                renderer1.setLayoutParams(layoutParams);
            }
        }
    }
}
