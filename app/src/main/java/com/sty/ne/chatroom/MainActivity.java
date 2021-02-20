package com.sty.ne.chatroom;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    private EditText etSignal;
    private EditText etPort;
    private EditText etRoom;
    private EditText etWss;
    private Button btnJoinRoom;
    private Button btnSingleVideo;
    private Button btnSingleAudio;
    private Button btnWssTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        addListeners();
    }

    private void initView() {
        etSignal = findViewById(R.id.et_signal);
        etPort = findViewById(R.id.et_port);
        etRoom = findViewById(R.id.et_room);
        etWss = findViewById(R.id.et_wss);
        btnJoinRoom = findViewById(R.id.btn_join_room);
        btnSingleVideo = findViewById(R.id.btn_single_video);
        btnSingleAudio = findViewById(R.id.btn_single_audio);
        btnWssTest = findViewById(R.id.btn_wss_test);
    }


    private void addListeners() {
        btnJoinRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebRTCUtil.call(MainActivity.this, etSignal.getText().toString(), etRoom.getText().toString());
                //WebRTCManager.getInstance().connect(MainActivity.this, etRoom.getText().toString());
            }
        });

        btnSingleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebRTCUtil.callSingle(MainActivity.this,
                        etSignal.getText().toString(),
                        etRoom.getText().toString().trim() + ":" + etPort.getText().toString().trim(),
                        true);
            }
        });

        btnSingleAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebRTCUtil.callSingle(MainActivity.this,
                        etSignal.getText().toString(),
                        etRoom.getText().toString().trim() + ":" + etPort.getText().toString().trim(),
                        false);
            }
        });

        btnWssTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebRTCUtil.testWs(etWss.getText().toString());
            }
        });
    }

}