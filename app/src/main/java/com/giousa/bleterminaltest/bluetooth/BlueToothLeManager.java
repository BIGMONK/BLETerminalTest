package com.giousa.bleterminaltest.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import com.giousa.bleterminaltest.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by black on 2016/4/12.
 */
public class BlueToothLeManager extends Thread implements BluetoothLeTool.BluetoothLeDataListener,
        BluetoothLeTool.BluetoothLeDiscoveredListener,
        BluetoothLeTool.BluetoothLeStatusListener{

    public interface SpeedChangedListener {
        void onSpeedChanged(short speed);
    }

    public interface AngleChangedListener {
        void onAngleChanged(float angle);
    }

    public interface HeartBeatChangedListener {
        void onHeartBeatChanged(int heartBeat);
    }

    private HeartBeatChangedListener mHeartBeatChangedListener;
    private SpeedChangedListener mSpeedChangedListener;
    private AngleChangedListener mAngleChangedListener;

    public void setAngleChangedListener(AngleChangedListener angleChangedListener) {
        mAngleChangedListener = angleChangedListener;
    }

    public void setSpeedChangedListener(SpeedChangedListener speedChangedListener) {
        mSpeedChangedListener = speedChangedListener;
    }

    public void setHeartBeatChangedListener(HeartBeatChangedListener heartBeatChangedListener) {
        mHeartBeatChangedListener = heartBeatChangedListener;
    }

    private final String TAG = BlueToothLeManager.class.getSimpleName();
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private ArrayList<BluetoothDevice> mLeDevices;
    private Map<String, BluetoothDevice> mDeviceMap = new HashMap<>();
    private BluetoothDevice mBluetoothDevice;
    private BluetoothLeTool mBluetoothTool;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
    private int mCharProp;
    private String mDeviceName;
    private long mLastSendTimestamps;

    private List<Integer> mSendDataList = null;

    public BlueToothLeManager(Context pContext) {
        mContext = pContext;
    }

    private boolean blueToothInit() {

        mHandler = new Handler();
        mLeDevices = new ArrayList<BluetoothDevice>();

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        Log.i(TAG, "mBluetoothAdapter=" + String.valueOf(mBluetoothAdapter));

        if (mBluetoothAdapter == null) {
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                if (mContext instanceof Activity)
                    ((Activity) mContext).startActivityForResult(enableBtIntent, 1);
            }
        }

        mSendDataList = new LinkedList<Integer>();

        mLastSendTimestamps = System.currentTimeMillis();

        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
////                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
////                    connectDevice();
//                }
//            }, 5000);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    UIUtils.runInMainThread(new Runnable() {
                        @Override
                        public void run() {
                            String deviceName = device.getName();

                            if (deviceName == null) {
                                return;
                            }

                            if (!mDeviceMap.containsKey(deviceName)) {
                                mDeviceMap.put(deviceName, device);

                                if (mDeviceName.equals(deviceName)) {
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    connectDevice();
                                }
                            }
                        }
                    });
                }
            };

    public BluetoothDevice getDevice() {
        int count = mLeDevices.size();
        for (int i = 0; i < count; i++) {
            if (mDeviceName == null)
                return null;

            Log.d(TAG, "devices : " + mLeDevices.get(i).getName() + " mDeviceName  : " + mDeviceName);


            if (mLeDevices.get(i).getName().equals(mDeviceName)) {
                return mLeDevices.get(i);
            }
        }
        return null;
    }

    public boolean connectDevice() {

        mBluetoothDevice = mDeviceMap.get(mDeviceName);

        if (mBluetoothDevice == null) {
            return false;
        }

        Log.d(TAG + "蓝牙设备名称：", String.valueOf(mBluetoothDevice.getName()) + "***");
        Log.d(TAG + "蓝牙设备名称：", "***" + mBluetoothDevice.getName());

        mBluetoothTool = new BluetoothLeTool();
        if (!mBluetoothTool.initialize()) {
            Log.i(TAG, "Unable to initialize Bluetooth");
            return false;
        }

        mBluetoothTool.connect(mBluetoothDevice.getAddress());
        mBluetoothTool.setBluetoothLeDataListener(this);
        mBluetoothTool.setBluetoothLeDiscoveredListener(this);
        mBluetoothTool.setBluetoothLeStatusListener(this);

        if (mBluetoothTool != null) {
            final boolean result = mBluetoothTool.connect(mBluetoothDevice.getAddress());
            Log.i(TAG, "Connect request result=" + result);
            return result;
        }
        return false;
    }

    public void disconnectDevice() {
        if (mBluetoothDevice != null)
            mBluetoothTool.disconnect();
    }

    public void sendData(int value) {
        synchronized (mSendDataList) {
//            if (mSendDataList.size() > 10000) {
//                mSendDataList.remove(0);
//            }
            mSendDataList.add(value);
        }
    }

    private void setCharacteristicNotification() {
        mBluetoothTool.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
    }

    private int count = 0;

    public void onDataAvailable(byte[] value) {

        Log.d(TAG,"getdata-------"+value.toString());

        mBluetoothTool.setCharacteristicNotification(mBluetoothGattCharacteristic, true);

        mBluetoothGattCharacteristic.setValue(""+count++);
        mBluetoothTool.writeCharacteristic(mBluetoothGattCharacteristic);

//        if (value[0] == 0x04 && value[7] == 0x05) {
//
//            synchronized (mSendDataList) {
//                if (mSendDataList != null && mSendDataList.size() > 0) {
//                    Integer sendData = mSendDataList.get(0);
//                    byte bys = sendData.byteValue();
//                    byte[] sendBuffer = new byte[]{4, 64, bys, 5};
//                    mBluetoothTool.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
//
//                    mBluetoothGattCharacteristic.setValue(sendBuffer);
//                    mBluetoothTool.writeCharacteristic(mBluetoothGattCharacteristic);
//
//                    mSendDataList.remove(0);
//                }
//            }
//        }
//
//        if (value[0] != 0x04 || value[7] != 0x05) {
//            return ;
//        }
//
//        if (System.currentTimeMillis() - mLastSendTimestamps > 50) {
//
//            UserInputInfo userInputInfo = parseUserInputInfo(value);
//
//            if (userInputInfo == null) {
//                return;
//            }
//
//            doMyWork(userInputInfo);
//
//            mLastSendTimestamps = System.currentTimeMillis();
//        }
    }

    private void doMyWork(UserInputInfo userInputInfo) {
        //如果有其它的变化，全部放到这儿来处理
        if ((userInputInfo.changeValueStatus & UserInputInfo.VALUE_CHANGED_HEARTRATE)
                == UserInputInfo.VALUE_CHANGED_HEARTRATE) {
            if (mHeartBeatChangedListener != null) {
                mHeartBeatChangedListener.onHeartBeatChanged(userInputInfo.heartBeat);
            }
        }

        if (mAngleChangedListener != null) {
            mAngleChangedListener.onAngleChanged(userInputInfo.angle);
        }

        if (mSpeedChangedListener != null) {
            mSpeedChangedListener.onSpeedChanged(userInputInfo.speed);
        }

    }

    public void initBlueToothInfo() {
        blueToothInit();
        scanLeDevice(true);
    }

    public void unintBlueToothInfo() {

    }

    public void onConnected() {
        Log.d(TAG, "****ACTION_GATT_CONNECTED********");
    }

    public void onDisconnected() {
        Log.d(TAG, "*********ACTION_GATT_DISCONNECTED***********");
    }

    public void onDiscovered(List<BluetoothGattService> supportedService) {
        List<BluetoothGattService> mGattServices = supportedService;

        for (int i = 0; i < mGattServices.size(); i++) {
            Log.d(TAG, "**********" + mGattServices.get(i).getUuid().toString());
            if (mGattServices.get(i).getUuid().toString().equals("0000ffe0-0000-1000-8000-00805f9b34fb")) {
                BluetoothGattService mBluetoothGattService = mGattServices.get(i);
                List<BluetoothGattCharacteristic> ListBlueChar = mBluetoothGattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : ListBlueChar) {
                    Log.d(TAG, "--------------" + characteristic.getProperties() + "");
                    if (characteristic != null) {
                        mBluetoothGattCharacteristic = characteristic;
                        setCharacteristicNotification();
                        mCharProp = characteristic.getProperties();
                    }
                }
            }
        }
    }

    public void setDeviceName(String name) {
        mDeviceName = name;
    }

    /**
     * Created by black on 2016/1/24 0024.
     */
    // TODO: 2016/6/30 0030 基本数据类 
    public static class UserInputInfo implements Cloneable {
        public static final int VALUE_CHANGED_SPEED = 0x00000008;
        public static final int VALUE_CHANGED_ANGLE = 0x00000010;
        public static final int VALUE_CHANGED_HEARTRATE = 0x00000040;

        public int changeValueStatus = 0;
        public short speed;
        public float angle;
        public int heartBeat;
        public int resistance;

        public UserInputInfo() {

        }

        public void resetChanged() {
            changeValueStatus = 0x00000000;
        }
    }

    //// TODO: 2016/6/30 0030 解析数据 
    private UserInputInfo parseUserInputInfo(byte[] inBuffer) {

        UserInputInfo userInputInfo = new UserInputInfo();
        boolean hasAnyChanged = false;

//        float speed = (short) (inBuffer[6]*256 + inBuffer[7]);//速度
//        float speed = (float) (inBuffer[2] * 256 + inBuffer[3]) / 100;//速度

        short speed = (short) (inBuffer[2] & 0xFF);
//        if(speed < 0){
//            speed = (short) (255+speed);
//        }

        if(speed == 0){
            userInputInfo.speed = 0;
            hasAnyChanged = true;
        }else{
            if (userInputInfo.speed != speed) {
                userInputInfo.speed = speed;
//                userInputInfo.changeValueStatus |= UserInputInfo.VALUE_CHANGED_SPEED;
                hasAnyChanged = true;
            }
        }




//        int heartBeat = inBuffer[8];//心率
        int heartBeat = inBuffer[4];//心率
        if (userInputInfo.heartBeat != heartBeat) {
            userInputInfo.heartBeat = heartBeat;
            userInputInfo.changeValueStatus |= UserInputInfo.VALUE_CHANGED_HEARTRATE;
            hasAnyChanged = true;
        }

        //float angle = (float)(((short) (inBuffer[9]*256 + inBuffer[10])-3200)/22.0f);//角度
//        float angle = (float) (inBuffer[5] * 256 + inBuffer[6]) / 100;//角度
        float angle = inBuffer[5];//角度
//        if (userInputInfo.angle != angle) {
//            userInputInfo.angle = angle;
//            userInputInfo.changeValueStatus |= UserInputInfo.VALUE_CHANGED_ANGLE;
//            hasAnyChanged = true;
//        }

        if(angle == 0){
            userInputInfo.angle = 0;
            hasAnyChanged = true;
        }else{
            if (userInputInfo.angle != angle) {
                userInputInfo.angle = angle;
//                userInputInfo.changeValueStatus |= UserInputInfo.VALUE_CHANGED_SPEED;
                hasAnyChanged = true;
            }
        }

        if (!hasAnyChanged) {
            return null;
        }

        return userInputInfo;
    }

}
