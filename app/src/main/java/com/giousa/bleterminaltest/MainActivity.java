package com.giousa.bleterminaltest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements InputSystemManager.HeartBeatSystemEventListener {

    private TextView mAchieveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

    }

    private int count = 0;

    private void initView() {
        mAchieveData = (TextView) findViewById(R.id.tv_achieve);
        Button sendData = (Button) findViewById(R.id.btn_send);

        final InputSystemManager inputSystemManager = InputSystemManager.getInstance();
        inputSystemManager.setHeartBeatSystemEventListener(this);

        sendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputSystemManager.sendData(count++);
            }
        });
    }

    @Override
    public void onHeartBeatChanged(final int heartBeat) {
        Log.d("MainActivity","heartBeat:"+heartBeat);
        UIUtils.runInMainThread(new Runnable() {
            @Override
            public void run() {
                mAchieveData.setText(""+heartBeat);
            }
        });
    }
}
