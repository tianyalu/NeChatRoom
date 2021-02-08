package com.sty.ne.chatroom.utils;

import android.content.Context;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/8 4:21 PM
 */
public class Utils {
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
