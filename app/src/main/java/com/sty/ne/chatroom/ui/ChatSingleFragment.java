package com.sty.ne.chatroom.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sty.ne.chatroom.R;
import com.sty.ne.chatroom.utils.ScreenUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * 单人聊天控制界面
 * @Author: tian
 * @UpdateDate: 2021/2/20 5:01 PM
 */
public class ChatSingleFragment extends Fragment {
    public View rootView;
    private TextView tvSwitchMute;
    private TextView tvSwitchHangUp;
    private TextView tvSwitchCamera;
    private TextView tvHangFree;
    private boolean enableMic = true;
    private boolean enableSpeaker = false;
    private boolean videoEnable;
    private ChatSingleActivity activity;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (ChatSingleActivity) getActivity();
        Bundle bundle = getArguments();
        if(bundle != null) {
            videoEnable = bundle.getBoolean("videoEnable");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(rootView == null) {
            rootView = onInitLoadView(inflater, container, savedInstanceState);
            initView(rootView);
            initListener();
        }
        return rootView;
    }

    private View onInitLoadView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wr_fragment_room_control_single, container, false);
    }

    private void initView(View rootView) {
        tvSwitchMute = rootView.findViewById(R.id.tv_switch_mute);
        tvSwitchHangUp = rootView.findViewById(R.id.tv_switch_hang_up);
        tvSwitchCamera = rootView.findViewById(R.id.tv_switch_camera);
        tvHangFree = rootView.findViewById(R.id.tv_hand_free);
        if(videoEnable) {
            tvHangFree.setVisibility(View.GONE);
            tvSwitchCamera.setVisibility(View.VISIBLE);
        }else {
            tvHangFree.setVisibility(View.VISIBLE);
            tvSwitchCamera.setVisibility(View.GONE);
        }
    }

    private void initListener() {
        tvSwitchMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableMic = !enableMic;
                Drawable drawable;
                if(enableMic) {
                    drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_mute_default);
                }else {
                    drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_mute);
                }
                if(drawable != null) {
                    drawable.setBounds(0, 0, ScreenUtils.dip2px(activity, 60),
                            ScreenUtils.dip2px(activity, 60));
                }
                tvSwitchMute.setCompoundDrawables(null, drawable, null, null);

                activity.toggleMic(enableMic);
            }
        });

        tvSwitchHangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.hangUp();
            }
        });

        tvSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.switchCamera();
            }
        });

        tvHangFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableSpeaker = !enableSpeaker;
                Drawable drawable;
                if(enableSpeaker) {
                    drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_hands_free);
                }else {
                    drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_hands_free_default);
                }
                if(drawable != null) {
                    drawable.setBounds(0, 0, ScreenUtils.dip2px(activity, 60),
                            ScreenUtils.dip2px(activity, 60));
                }
                tvHangFree.setCompoundDrawables(null, drawable, null, null);

                activity.toggleSpeaker(enableSpeaker);
            }
        });
    }

}
