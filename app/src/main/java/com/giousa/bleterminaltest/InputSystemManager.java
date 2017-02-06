package com.giousa.bleterminaltest;

import android.content.Context;
import android.view.GestureDetector;

import com.giousa.bleterminaltest.bluetooth.BlueToothLeManager;

/**
 * Created by liuenbao on 1/23/16.
 */
public class InputSystemManager extends GestureDetector.SimpleOnGestureListener
        implements BlueToothLeManager.HeartBeatChangedListener {

    private static InputSystemManager instance;
    private BlueToothLeManager mBlueToothLeManager;


    public interface HeartBeatSystemEventListener{
        void onHeartBeatChanged(int heartBeat);
    }

    private HeartBeatSystemEventListener mHeartBeatSystemEventListener;

    public void setHeartBeatSystemEventListener(HeartBeatSystemEventListener heartBeatSystemEventListener) {
        mHeartBeatSystemEventListener = heartBeatSystemEventListener;
    }

    public static InputSystemManager getInstance() {
        if (instance == null) {
            instance = new InputSystemManager();
        }
        return instance;
    }

    //蓝牙消息
    @Override
    public void onHeartBeatChanged(int heartBeat) {
        mHeartBeatSystemEventListener.onHeartBeatChanged(heartBeat);
    }


    public boolean initWithContext(Context context) {

        mBlueToothLeManager = new BlueToothLeManager(context);
        mBlueToothLeManager.initBlueToothInfo();
//        设置设备的名称，一台设备只能有一个名字
        mBlueToothLeManager.setDeviceName("HMSoft");

//        设置监听事件
        mBlueToothLeManager.setHeartBeatChangedListener(this);
        return true;
    }

    public void sendData(int value){
        mBlueToothLeManager.sendData(value);
    }

}
