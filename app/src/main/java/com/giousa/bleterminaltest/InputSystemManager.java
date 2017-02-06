package com.giousa.bleterminaltest;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.view.GestureDetector;

import com.giousa.bleterminaltest.bluetooth.BlueToothLeManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by liuenbao on 1/23/16.
 */
public class InputSystemManager extends GestureDetector.SimpleOnGestureListener
        implements BlueToothLeManager.HeartBeatChangedListener {

    private static final String TAG = InputSystemManager.class.getSimpleName();

    private static InputSystemManager instance;

    public static final int EMPTY = 0;
    public static final int LEFT = 1;
    public static final int UP = 2;
    public static final int RIGHT = 3;
    public static final int DOWN = 4;
    public static final int ENTER = 5;
    public static final int BACK = 6;

    private Context mContext;
    private BlueToothLeManager mBlueToothTools;

    private Handler mHandler = new Handler();

    //An array of observers
    private List<HeartBeatSystemEventListener> mHeartBeatSystemEventListeners;

    private InputSystemManager() {
        mHeartBeatSystemEventListeners = new LinkedList<>();
    }

    public static InputSystemManager getInstance() {
        if (instance == null) {
            instance = new InputSystemManager();
        }
        return instance;
    }

    //蓝牙消息
    @Override
    public void onHeartBeatChanged(final int heartBeat) {
        UIUtils.runInMainThread(new Runnable() {
            @Override
            public void run() {
                if (mHeartBeatSystemEventListeners != null) {
                    for (HeartBeatSystemEventListener listener : mHeartBeatSystemEventListeners) {
                        listener.onHeartBeatChanged(InputSystemManager.this, heartBeat);
                    }
                }
            }
        });
    }

    //Input System Public Interface begin
    //若在某个Activity里面监听该事件需要继承这些listener

    public interface HeartBeatSystemEventListener{
        void onHeartBeatChanged(InputSystemManager inputSystemManager, int heartBeat);
    }

    //注意，此处的Context一定是Activity的context
    //// TODO: 2016/6/30 0030 初始化蓝牙信息，以及监听事件
    public boolean initWithContext(Context context) {

        //这里的context只能是Activity的context
        mContext = context;

//        registerWifiStatusManager();
        mBlueToothTools = new BlueToothLeManager(context);
        mBlueToothTools.initBlueToothInfo();
//        设置设备的名称，一台设备只能有一个名字
        mBlueToothTools.setDeviceName("HMSoft");

//        设置监听事件
        mBlueToothTools.setHeartBeatChangedListener(this);
        return true;
    }

}
