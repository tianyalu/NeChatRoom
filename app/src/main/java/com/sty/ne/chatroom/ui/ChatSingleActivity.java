package com.sty.ne.chatroom.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.sty.ne.chatroom.ProxyVideoSink;
import com.sty.ne.chatroom.R;
import com.sty.ne.chatroom.WebRTCManager;
import com.sty.ne.chatroom.interfaces.IViewCallback;
import com.sty.ne.chatroom.utils.PermissionUtil;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * 单聊界面：
 * 1. 一对一视频通话
 * 2. 一对一语言通话
 * @Author: tian
 * @UpdateDate: 2021/2/20 4:46 PM
 */
public class ChatSingleActivity extends AppCompatActivity {
    private static final String TAG = ChatSingleActivity.class.getSimpleName();
    private SurfaceViewRenderer svrLocalView;
    private SurfaceViewRenderer svrRemoteView;
    private ProxyVideoSink localRender;
    private ProxyVideoSink remoteRender;

    private WebRTCManager rtcManager;

    private boolean videoEnable;
    private boolean isSwappedFeeds;

    private EglBase rootEglBase;

    private int previewX, previewY;
    private int moveX, moveY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_single);
        initData();
        initView();
        initListeners();
    }

    public static void openActivity(Activity activity, boolean videoEnable) {
        Intent intent = new Intent(activity, ChatSingleActivity.class);
        intent.putExtra("videoEnable", videoEnable);
        activity.startActivity(intent);
    }

    private void initData() {
        Intent intent = getIntent();
        videoEnable = intent.getBooleanExtra("videoEnable", false);
    }

    private void initView() {
        ChatSingleFragment chatSingleFragment = new ChatSingleFragment();
        replaceFragment(chatSingleFragment, videoEnable);
        rootEglBase = EglBase.create();
        if(videoEnable) {
            svrLocalView = findViewById(R.id.svr_local_render);
            svrRemoteView = findViewById(R.id.svr_remote_render);

            //本地图像初始化
            svrLocalView.init(rootEglBase.getEglBaseContext(), null);
            svrLocalView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            svrLocalView.setZOrderMediaOverlay(true);
            svrLocalView.setMirror(true);
            localRender = new ProxyVideoSink();

            //远端图像初始化
            svrRemoteView.init(rootEglBase.getEglBaseContext(), null);
            svrRemoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
            svrRemoteView.setMirror(true);
            remoteRender = new ProxyVideoSink();
            setSwappedFeeds(true);

            svrLocalView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSwappedFeeds(!isSwappedFeeds);
                }
            });
        }

        startCall();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListeners() {
        if(videoEnable) {
            //设置小视频可以移动
            svrLocalView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            previewX = (int) event.getX();
                            previewY = (int) event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            int x = (int) event.getX();
                            int y = (int) event.getY();
                            moveX = x;
                            moveY = y;
                            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) svrLocalView.getLayoutParams();
                            //Clears the rule, as there is no removeRule until API 17;
                            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                            lp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
                            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                            lp.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                            int left = lp.leftMargin + (x - previewX);
                            int top = lp.topMargin + (y - previewY);
                            lp.leftMargin = left;
                            lp.topMargin = top;
                            view.setLayoutParams(lp);
                            break;
                        case MotionEvent.ACTION_UP:
                            if(moveX == 0 && moveY == 0) {
                                view.performClick();
                            }
                            moveX = 0;
                            moveY = 0;
                            break;
                        default:
                            break;

                    }
                    return true;
                }
            });
        }
    }

    private void replaceFragment(Fragment fragment, boolean videoEnable) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("videoEnable", videoEnable);
        fragment.setArguments(bundle);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.rl_container, fragment)
                .commit();
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        this.isSwappedFeeds = isSwappedFeeds;
        localRender.setTarget(isSwappedFeeds ? svrRemoteView : svrLocalView);
        remoteRender.setTarget(isSwappedFeeds ? svrLocalView : svrRemoteView);
    }

    private void startCall() {
        rtcManager = WebRTCManager.getInstance();
        rtcManager.setViewCallback(new IViewCallback() {
            @Override
            public void onSetLocalStream(MediaStream stream, String socketId) {
                if(stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(localRender);
                }

                if(videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                }
            }

            @Override
            public void onAddRemoteStream(MediaStream stream, String socketId) {
                if(stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(remoteRender);
                }
                if(videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setSwappedFeeds(false);
                        }
                    });
                }
            }

            @Override
            public void onCloseWithId(String socketId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        disconnect();
                        ChatSingleActivity.this.finish();
                    }
                });
            }
        });

        if(!PermissionUtil.isNeedRequestPermission(ChatSingleActivity.this)) {
//            rtcManager.joinRoom(ChatSingleActivity.this, rootEglBase);
            rtcManager.joinRoom(getApplicationContext(), rootEglBase);
        }
    }

    private void disconnect() {
        rtcManager.exitRoom();
        if(localRender != null) {
            localRender.setTarget(null);
            localRender = null;
        }
        if(remoteRender != null) {
            remoteRender.setTarget(null);
            remoteRender = null;
        }

        if(svrLocalView != null) {
            svrLocalView.release();
            svrLocalView = null;
        }
        if(svrRemoteView != null) {
            svrRemoteView.release();
            svrRemoteView = null;
        }
    }

    //静音
    public void toggleMic(boolean enableMic) {
        rtcManager.toggleMute(enableMic);
    }

    //挂断
    public void hangUp() {
        disconnect();
        this.finish();
    }

    public void switchCamera() {
        rtcManager.switchCamera();
    }

    //扬声器
    public void toggleSpeaker(boolean enableSpeaker) {
        rtcManager.toggleLarge(enableSpeaker);
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
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
        rtcManager.joinRoom(getApplicationContext(), rootEglBase);

    }
}
