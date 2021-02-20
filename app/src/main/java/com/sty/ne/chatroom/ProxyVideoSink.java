package com.sty.ne.chatroom;

import android.util.Log;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/18 6:09 PM
 */
public class ProxyVideoSink implements VideoSink {
    private static final String TAG = ProxyVideoSink.class.getSimpleName();
    private VideoSink target;

    @Override
    public synchronized void onFrame(VideoFrame videoFrame) {
        if(target == null) {
            Log.e(TAG, "Dropping frame in proxy because target is null.");
            return;
        }
        target.onFrame(videoFrame);
    }

    public synchronized void setTarget(VideoSink target) {
        this.target = target;
    }
}
