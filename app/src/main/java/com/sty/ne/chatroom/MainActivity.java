package com.sty.ne.chatroom;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    private EditText etRoom;
    private Button btnJoinRoom;
    private Button btnSingleVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        addListeners();
    }

    private void initView() {
        etRoom = findViewById(R.id.et_room);
        btnJoinRoom = findViewById(R.id.btn_join_room);
        btnSingleVideo = findViewById(R.id.btn_single_video);
    }


    private void addListeners() {
        WebRTCManager.getInstance().connect(this, etRoom.getText().toString());
    }
}