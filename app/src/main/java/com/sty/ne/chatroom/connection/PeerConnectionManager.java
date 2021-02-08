package com.sty.ne.chatroom.connection;

import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理p2p通讯的类
 *
 * @Author: tian
 * @UpdateDate: 2021/2/5 10:24 PM
 */
public class PeerConnectionManager {
    private List<PeerConnection> peerConnections;

    private static final class LazyHolder {
        private static PeerConnectionManager INSTANCE = new PeerConnectionManager();
    }

    private PeerConnectionManager() {
        peerConnections = new ArrayList<>();
    }

    public static PeerConnectionManager getInstance() {
        return LazyHolder.INSTANCE;
    }
}
