package com.example.bletest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by larry on 15/7/17.
 */
public class DataTransmission {
    private static final String TAG = "BluetoothLE";
    private static final int maximumPktNum = 120;
    private static final int timeout = 5000;

    private Activity activity = null;
    private BluetoothLE ble = null;
    private ImageDetection imageDetection = null;

    public File mainStorage = null;
    public File file;
    private FileOutputStream fos;

    private byte [][] picBuf;
    private byte [] tempBuf;
    private int pktNum = 0;
    private int lastPktSize = 0;

    private int tempPktId = 0;
    private int recvNum = 0;
    private int bufOffset = 0;
    private boolean picInfoPktRecv = false;

    private Set<Integer> integerSet;

    private Timer timer = null;
    private int counter = 0;


    public DataTransmission(Activity activity, BluetoothLE ble){
        this.activity = activity;
        this.ble = ble;
        imageDetection = new ImageDetection(this.activity, this);
        tempBuf = new byte [128];
        picBuf = new byte [maximumPktNum][];
        integerSet = new HashSet();
    }

    public void parsePackets(byte [] data){
        int seqNum = (data[2] & 0xFF) * 256 + (data[1] & 0xFF);

        // Checksum for BLE packet
        int checksum = 0;
        for(int i = 0; i < data.length-1; i++){
            checksum += (data[i] & 0xFF);
            checksum = checksum & 0xFF;
        }

        if (checksum != (data[data.length-1] & 0xFF)){
            Log.d(TAG, "Checksum error on ble packets ".concat(String.valueOf(seqNum)));
        }

        if( seqNum == 0x7FFF){
            if( picInfoPktRecv == false ) {
                picInfoPktRecv = true;
                tempPktId = 0;
                bufOffset = 0;
                int picTotalLen = (data[4] & 0xFF) * 256 + (data[3] & 0xFF);
                pktNum = picTotalLen / (128 - 6);
                if (pktNum % (128 - 6) != 0) {
                    pktNum++;
                    lastPktSize = picTotalLen % (128 - 6) + 6;
                }
                Log.d(TAG, "Total picture length:".concat(String.valueOf(picTotalLen)));
                Log.d(TAG, "Total packets:".concat(String.valueOf(pktNum)));
                Log.d(TAG, "Last packet size:".concat(String.valueOf(lastPktSize)));

                if (mainStorage == null) {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                        mainStorage = new File(Environment.getExternalStorageDirectory(), "TempPicDir");
                    else
                        mainStorage = new File(activity.getApplicationContext().getFilesDir(), "TempPicDir");
                }
                if (!mainStorage.exists())
                    mainStorage.mkdirs();

                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                file = new File(mainStorage, "PIC_".concat(String.valueOf(timestamp)).concat(".jpg"));
                try {
                    fos = new FileOutputStream(file, true);
                } catch (IOException e) {
                    Log.d(TAG, "FAIL TO OPEN");
                    fos = null;
                }
            }
            else{
                ble.bleWriteAck((byte) 0x05);
                timer = new Timer();
                timer.schedule(new TimeoutTask(), timeout);
                counter = 0;
            }
        }
        else{
            if(bufOffset == 0){
                tempPktId = seqNum/8;
            }

            if(integerSet.contains(tempPktId)){
                Log.i(TAG, tempPktId + "has been received repeatedly.");
                return;
            }

            if( bufOffset/16 != seqNum %8) {
                Log.d(TAG, "Packet is recieved.".concat(String.valueOf(bufOffset / 16)).concat(String.valueOf(seqNum % 8)));
                bufOffset = 0;
                return;
            }

            System.arraycopy(data, 3, tempBuf, bufOffset, data.length - 4);
            bufOffset += (data.length - 4);

            if ( bufOffset == 128 || ((tempPktId == pktNum - 1) && bufOffset == lastPktSize) ) {


                int sum = 0;
                for(int i = 0; i < bufOffset-2; i++){
                    sum += (tempBuf[i] & 0xFF);
                    sum = sum & 0xFF;
                }

                if (( sum & 0xFF ) == (tempBuf[bufOffset-2] & 0xFF) ){
                    Log.d(TAG, String.valueOf(tempPktId + 1).concat(" packet recieved."));
                    picBuf[tempPktId] = new byte[bufOffset - 6];
                    System.arraycopy(tempBuf, 4, picBuf[tempPktId], 0, bufOffset - 6);

                    integerSet.add(tempPktId);
                    recvNum++;
                    bufOffset = 0;

                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new TimeoutTask(), timeout);

                    Bundle countBundle = new Bundle();
                    countBundle.putFloat("progress", (float) recvNum*100/pktNum);

                    Message msg = new Message();
                    msg.what = MainActivity.UPDATE_PROGRESS_MSG;
                    msg.setData(countBundle);
                    ((MainActivity) activity).mHandler.sendMessage(msg);

//                    if(tempPktId == pktNum - 1){
//                        checkPackets();
//                    }
                }else{
                    Log.d(TAG, "Checksum error in ".concat(String.valueOf(tempPktId)).concat("th packet."));
                    bufOffset = 0;
                }
            }
        }
    }

    public void checkPackets(){
        Log.i(TAG, "Dropout rate: " + (float) (pktNum - recvNum) * 100 / pktNum + "%");
        if(recvNum == pktNum){
            try {
                int currentIdx = 0;
                byte [] pictureBytes = new byte [(pktNum-1) * 122 + (lastPktSize - 6)];
                for(int i = 0; i < pktNum; i++) {
                    System.arraycopy(picBuf[i], 0, pictureBytes, currentIdx, picBuf[i].length);
                    fos.write(picBuf[i]);
                    currentIdx += picBuf[i].length;
                }

                /* Byte array to bitmap*/
                BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
                bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                ByteArrayInputStream inputStream = new ByteArrayInputStream(pictureBytes);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, bmpFactoryOptions);



                bufOffset = 0;
                picInfoPktRecv = false;



                try {
                    fos.close();

                    ble.bleWriteState((byte) 0x07);
                    timer.cancel();
                    timer = null;

                    if(((MainActivity) activity).getPictureState() == MainActivity.PRECAPTURE_STATE){
                        imageDetection.roiDetectionOnWhite(bitmap);
                    }
                    else{
                        //imageDetection.roiDetection(bitmap);  // Previous recognition method
                        Bundle countBundle = new Bundle();
                        countBundle.putString("picturePath", file.getAbsolutePath());

                        Message msg = new Message();
                        msg.what = MainActivity.PICTURE_PREVIEW_MSG;
                        msg.setData(countBundle);
                        ((MainActivity) activity).mHandler.sendMessage(msg);

                        imageDetection.testStripDetection(bitmap);
                    }



                    resetParameters();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
        else{
            int remainPktNum = pktNum - recvNum;
            if(remainPktNum > 18)
                remainPktNum = 18;

            byte [] bytes = new byte [20];
            bytes[0] = (byte)0xA3;
            bytes[1] = (byte)(remainPktNum & 0xFF);
            int j = 0;
            for(int i = 0; i < remainPktNum; i++){
                for(;j < pktNum; j++){
                    if(!integerSet.contains(j)){
                        Log.i(TAG, "Lost " + j + " th packet");
                        bytes[i+2] = (byte)(j & 0xFF);
                        j++;
                        break;
                    }
                }
            }

            ble.bleWriteData(bytes);
        }
    }

    public void resetParameters(){
        Log.i(TAG, "Reset parameters of data transmission.");
        picInfoPktRecv = false;
        recvNum = 0;
        bufOffset = 0;

        for(int i = 0; i < maximumPktNum; i++){
            picBuf[i] = null;
        }
        integerSet.clear();
    }

    public void resetTimeoutTimer(){
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimeoutTask(), timeout);
    }

    class TimeoutTask extends TimerTask{

        @Override
        public void run() {
            if(counter < 10){
                if(ble != null) {
                    Log.i(TAG, "Timeout timer was fired " + counter);
                    checkPackets();
                    counter++;
                    resetTimeoutTimer();
                }
            }
            else{
                timer.cancel();
                Bundle countBundle = new Bundle();
                countBundle.putFloat("dropout", (float) (pktNum - recvNum) / pktNum);

                Message msg = new Message();
                msg.what = MainActivity.DATA_TRANSFER_FAILURE_MSG;
                msg.setData(countBundle);
                ((MainActivity) activity).mHandler.sendMessage(msg);
            }
        }
    }
}
