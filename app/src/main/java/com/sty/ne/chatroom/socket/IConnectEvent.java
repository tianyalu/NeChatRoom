package com.sty.ne.chatroom.socket;

/**
 * @Author: tian
 * @UpdateDate: 2021/2/19 4:05 PM
 */
public interface IConnectEvent {
    void onSuccess();

    void onFailed(String msg);
}
