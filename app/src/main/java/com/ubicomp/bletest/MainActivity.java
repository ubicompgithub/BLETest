package com.ubicomp.bletest;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements BluetoothListener {

    private static final String TAG = "BluetoothLE";
    private static final double APP_VERSION = 1.00;

    public static final int PRECAPTURE_STATE = 0;
    public static final int CAPTURE_STATE = 1;

    public static final int UPDATE_PROGRESS_MSG     = 0;
    public static final int PICTURE_PREVIEW_MSG     = 1;
    public static final int SHOW_PREDICTION_MSG     = 2;
    public static final int DATA_TRANSFER_FAILURE_MSG   = 3;
    public static final int SHOW_AVG_GRAY_VALUE = 4;


    MainActivity mainActivity = this;
    private BluetoothLE ble = null;

    public Handler mHandler;

    private TextView progressDisplay;
    private TextView idDisplay;
    private TextView versionDisplay;
    private TextView detectionResult;
    private TextView avgROI;
    private Button buttonStart;
    private Button buttonPreCap;
    private Button buttonCap;
    private Button buttonClose;
    private Button buttonReset;
    private Button buttonWriteId;
    private ImageView imgPreview;
    private EditText editTextId;

    private int pictureState = MainActivity.CAPTURE_STATE;

    static {
        System.loadLibrary("opencv_java");
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();

        buttonStart = (Button)findViewById(R.id.buttonStart);
        buttonPreCap = (Button)findViewById(R.id.buttonPreCap);
        buttonCap = (Button)findViewById(R.id.buttonCap);
        buttonClose = (Button)findViewById(R.id.buttonClose);
        buttonReset = (Button)findViewById(R.id.buttonReset);
        buttonWriteId = (Button)findViewById(R.id.buttonWriteId);
        progressDisplay = (TextView)findViewById(R.id.progressDisplay);
        idDisplay = (TextView)findViewById(R.id.idDisplay);
        versionDisplay = (TextView)findViewById(R.id.versionDisplay);
        detectionResult = (TextView)findViewById(R.id.detectionResult);
        avgROI = (TextView)findViewById(R.id.avgROI);
        imgPreview = (ImageView)findViewById(R.id.imgPreview);
        editTextId = (EditText)findViewById(R.id.editArea);

        mHandler = new Handler(){

            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what){
                    case UPDATE_PROGRESS_MSG:
                        updateProcessRate(msg.getData().getFloat("progress"));
                        break;
                    case PICTURE_PREVIEW_MSG:
                        Bitmap bitmap = BitmapFactory.decodeFile(msg.getData().getString("picturePath"));
                        bleTakePictureSuccess(bitmap);
                        setImgPreview(bitmap);
                        break;
                    case SHOW_PREDICTION_MSG:
                        showDetectionResult(msg.getData().getFloat("check"));
                        break;
                    case DATA_TRANSFER_FAILURE_MSG:
                        bleTakePictureFail(msg.getData().getFloat("dropout"));
                        break;
                    case SHOW_AVG_GRAY_VALUE:
                        showAvgGrayValue(msg.getData().getFloat("average"));
                        break;
                }
            }
        };

        progressDisplay.setText("");
        idDisplay.setText("");
        versionDisplay.setText("");
        detectionResult.setText("");
        avgROI.setText("");
        editTextId.setInputType(InputType.TYPE_CLASS_PHONE);

        buttonStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ble != null) {
                    return;
                }
                //ble = new BluetoothLE(mainActivity, "ket_004");
                String inputStr = editTextId.getText().toString();
                Log.d(TAG, "Input str = " + inputStr);
                if (inputStr == ""){
                    return;
                }
                try {
                    int inputNum = Integer.valueOf(inputStr);
                    ble = new BluetoothLE(mainActivity, "ket_" + (inputNum % 1000) / 100 + (inputNum % 100) / 10 + inputNum % 10);
                    ble.bleConnect();
                }
                catch(Exception e){
                    e.printStackTrace();
                    return;
                }
                //testOpencv();
            }

        });

        buttonPreCap.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if (ble != null) {
                    ble.bleWriteState((byte) 0x03);
                    pictureState = MainActivity.PRECAPTURE_STATE;
                    buttonPreCap.setEnabled(false);
                    buttonCap.setEnabled(false);
                    clearProgressRate();
                    imgPreview.setImageDrawable(null);
                    //detectionResult.setText("");
                }
            }
        });
        
        buttonCap.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ble != null) {
                    ble.bleWriteState((byte) 0x06);
                    pictureState = MainActivity.CAPTURE_STATE;
                    buttonCap.setEnabled(false);
                    buttonPreCap.setEnabled(false);
                    clearProgressRate();
                    imgPreview.setImageDrawable(null);
                    detectionResult.setText("");
                    avgROI.setText("");
                }
            }

        });

        buttonClose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ble != null) {
                    ble.bleDisconnect();
                    ble = null;
                }
            }

        });

        buttonReset.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if(ble != null){
                    ble.bleWriteState((byte)0x01);
                    buttonPreCap.setEnabled(true);
                    buttonCap.setEnabled(true);
                    clearProgressRate();
                }
            }
        });

        buttonWriteId.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if (ble != null) {
                    String inputStr = editTextId.getText().toString();
                    int inputNum = Integer.valueOf(inputStr);
                    Log.i(TAG, "Input Id: " + inputNum);
                    byte [] bytes = new byte[20];
                    int temp = inputNum;
                    for(int i = 0; i < 4; i++){
                        bytes[4-i] = (byte) ((temp % 256) & 0xFF);
                        temp /= 256;
                    }
                    bytes[0] = (byte) 0xA1;
                    ble.bleWriteData(bytes);
                }
            }
        });

        buttonPreCap.setEnabled(false);
        buttonCap.setEnabled(false);
        buttonClose.setEnabled(false);
        buttonReset.setEnabled(false);
        buttonWriteId.setEnabled(false);
        Log.i(TAG, "On create");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(ble != null) {
            ble.bleDisconnect();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ble.onBleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void bleNotSupported() {
//        this.finish();
    }

    @Override
    public void bleConnectionTimeout() {
        Toast.makeText(this, "BLE connection timeout", Toast.LENGTH_SHORT).show();
        if(ble != null) {
            ble = null;
        }
    }

    @Override
    public void bleConnected() {
    	Toast.makeText(this, "BLE connected", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "BLE connected");
        Timer timer = new Timer();
        timer.schedule(new TimerTask(){

            @Override
            public void run() {
                if(ble != null) {
                    ble.bleWriteState((byte) 0x0A);
                }
            }
        }, 2000);
        buttonStart.setEnabled(false);
        buttonPreCap.setEnabled(true);
        buttonCap.setEnabled(true);
        buttonClose.setEnabled(true);
        buttonReset.setEnabled(true);
        buttonWriteId.setEnabled(true);
    }

    @Override
    public void bleDisconnected() {
    	Toast.makeText(this, "BLE disconnected", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "BLE disconnected");
        if(ble != null) {
            ble = null;
        }
        buttonStart.setEnabled(true);
        buttonPreCap.setEnabled(false);
        buttonCap.setEnabled(false);
        buttonClose.setEnabled(false);
        buttonReset.setEnabled(false);
        buttonWriteId.setEnabled(false);
        clearProgressRate();
        idDisplay.setText("");
        versionDisplay.setText("");
        detectionResult.setText("");
        avgROI.setText("");
    }

    @Override
    public void bleWriteStateSuccess() {
    	Toast.makeText(this, "BLE ACTION_DATA_WRITE_SUCCESS", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "BLE ACTION_DATA_WRITE_SUCCESS");
    }

    @Override
    public void bleWriteStateFail() {
    	Toast.makeText(this, "BLE ACTION_DATA_WRITE_FAIL", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "BLE ACTION_DATA_WRITE_FAIL");
    }

    @Override
    public void bleNoPlug() {
    	Toast.makeText(this, "No test plug", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "No test plug");
    }

    @Override
    public void blePlugInserted(byte[] plugId) {
    	//Toast.makeText(this, "Test plug is inserted", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Test plug is inserted");
    }

    @Override
    public void bleColorReadings(byte[] colorReadings) {
    	//Toast.makeText(this, "Color sensor readings", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Color sensor readings");
    }

    @Override
    public void displayCurrentId(String id) {
        idDisplay.setText("Saliva Id: " + id);
    }

    @Override
    public void displayCurrentDeviceVersion(int version) {
        versionDisplay.setText("Device code version: " + version);
        if(ble != null){
            ble.bleWriteState((byte) 0x01);
        }
    }

    @Override
	public void bleElectrodeAdcReading(byte state, byte[] adcReading) {
		// TODO Auto-generated method stub
	}

    public void updateProcessRate(float rate) {
        Double truncatedDouble = new BigDecimal(rate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        progressDisplay.setText(String.valueOf(truncatedDouble).concat(" %"));
    }

    public int getPictureState(){
        return pictureState;
    }

    public void setImgPreview(Bitmap bitmap) {
        imgPreview.setImageBitmap(bitmap);
    }

    public void clearProgressRate() {
        progressDisplay.setText("");
    }

    public void showDetectionResult(float score) {
        detectionResult.setText("Detection Score: " + score);
    }

    public void bleTakePictureSuccess(Bitmap bitmap) {
        //Toast.makeText(this, "Take picture successfully.", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Take picture successfully.");
        buttonPreCap.setEnabled(true);
        buttonCap.setEnabled(true);
    }

    public void bleTakePictureFail(float dropRate) {
        //Toast.makeText(this, "Take picture failed.", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Take picture failed.");
    }

    public void showAvgGrayValue(float value){
        Double truncatedDouble = new BigDecimal(value).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
        avgROI.setText("GrayValueAvg: " + truncatedDouble);
    }
}
