package com.sty.ne.chatroom.utils;

import android.content.Context;
import android.view.WindowManager;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/8 4:21 PM
 */
public class ScreenUtils {
    //屏幕宽度
    private static int mScreenWidth;

    public static void init(Context context) {
        if(mScreenWidth == 0) {
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if(manager != null) {
                mScreenWidth = manager.getDefaultDisplay().getWidth();
            }
        }
    }

    public static int getWidth(int size) {
        //只有4个人及其以下
        if(size <= 4) {
            return mScreenWidth / 2;
        }else {
            return mScreenWidth / 3;
        }
    }

    public static int getX(int size, int index) {
        if(size <= 4) {
            if(size == 3 && index == 2) { //会议室只有3个人的时候，第三个人的x偏移
                return mScreenWidth / 4;
            }
            return (index % 2) * mScreenWidth / 2;
        }else if(size <= 9) {
            if(size == 5) { //当会议室有5个人的时候
                if(index == 3) {
                    return mScreenWidth / 6;
                }
                if(index == 4) {
                    return mScreenWidth / 2;
                }
            }
            if(size == 7 && index == 6) { //当会议室有7个人的时候
                return mScreenWidth / 3;
            }
            if(size == 8) {
                if(index == 6) {
                    return mScreenWidth / 6;
                }
                if(index == 7) {
                    return mScreenWidth / 2;
                }
            }
            return (index % 3) * mScreenWidth / 3;
        }

        return 0;
    }

    public static int getY(int size, int index) {
        if(size < 3) {
            return mScreenWidth / 4;
        }else if(size < 5) {
            if(index < 2) {
                return 0;
            }else {
                return mScreenWidth / 2;
            }
        }else if(size < 7) {
            if(index < 3) {
                return mScreenWidth / 2 - (mScreenWidth / 3);
            }else {
                return mScreenWidth / 2;
            }
        }else if(size <= 9) {
            if(index < 3) {
                return 0;
            }else if(index < 6) {
                return mScreenWidth / 3;
            }else {
                return mScreenWidth / 3 * 2;
            }
        }
        return 0;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
