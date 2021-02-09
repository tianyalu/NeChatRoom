package com.sty.ne.chatroom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.sty.ne.chatroom.utils.PermissionUtil;

import org.webrtc.EglBase;

import androidx.annotation.Nullable;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/8 4:07 PM
 */
public class ChatRoomActivity extends Activity {
    private FrameLayout frVideoLayout;
    private WebRTCManager webRTCManager;
    private EglBase rootEglBase;

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
}
