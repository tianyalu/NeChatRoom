package com.sty.ne.chatroom.app;

import android.app.Application;

import com.sty.ne.chatroom.utils.ScreenUtils;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/9 4:56 PM
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ScreenUtils.init(this);
    }
}
