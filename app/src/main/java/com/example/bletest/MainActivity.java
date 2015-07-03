package com.example.bletest;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends Activity implements BluetoothListener {

    private static final String TAG = "BluetoothLE";

	private BluetoothLE ble = null;
    MainActivity mainActivity = this;
//    private EditText et_device;
    private Button buttonStart;
    private Button buttonSend;
    private Button buttonClose;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        buttonStart = (Button)findViewById(R.id.buttonStart);
        buttonSend = (Button)findViewById(R.id.buttonSend);
        buttonClose = (Button)findViewById(R.id.buttonClose);
        Button buttonReset = (Button)findViewById(R.id.buttonReset);
//        et_device = (EditText)findViewById(R.id.device_name);
        
        buttonStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(ble != null) {
                    return;
                }
                ble = new BluetoothLE(mainActivity, "ket_002");
                //ble = new BluetoothLE(mainActivity, et_device.getText().toString());
                ble.bleConnect();
            }

        });
        
        buttonSend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(ble != null){
                    ble.bleWriteState((byte)0x06);
                    buttonSend.setEnabled(false);
                }
            }

        });

        buttonClose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(ble != null) {
                    ble.bleDisconnect();
                    ble = null;
                }
            }

        });
        buttonSend.setEnabled(false);
        buttonClose.setEnabled(false);

        buttonReset.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if(ble != null){
                    ble.bleWriteState((byte)0x00);
                }
            }
        });

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
        buttonStart.setEnabled(false);
        buttonSend.setEnabled(true);
        buttonClose.setEnabled(true);
    }

    @Override
    public void bleDisconnected() {
    	Toast.makeText(this, "BLE disconnected", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "BLE disconnected");
        if(ble != null) {
            ble = null;
        }
        buttonStart.setEnabled(true);
        buttonSend.setEnabled(false);
        buttonClose.setEnabled(false);
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
    	Toast.makeText(this, "Color sensor readings", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Color sensor readings");
    }

    @Override
    public void bleTakePictureSuccess() {
        Toast.makeText(this, "Take picture successfully.", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Take picture successfully.");
        buttonSend.setEnabled(true);
    }

    @Override
	public void bleElectrodeAdcReading(byte state, byte[] adcReading) {
		// TODO Auto-generated method stub
	}
}
