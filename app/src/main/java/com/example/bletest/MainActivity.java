package com.example.bletest;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.Vector;

public class MainActivity extends Activity implements BluetoothListener {

    private static final String TAG = "BluetoothLE";

	private BluetoothLE ble = null;
    MainActivity mainActivity = this;
    private TextView progressBar;
    private TextView idBar;
    private Button buttonStart;
    private Button buttonSend;
    private Button buttonClose;
    private Button buttonReset;
    private Button buttonWriteId;
    private ImageView imageView1;
    private EditText editTextId;

    static {
        System.loadLibrary("opencv_java");
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();

        buttonStart = (Button)findViewById(R.id.buttonStart);
        buttonSend = (Button)findViewById(R.id.buttonSend);
        buttonClose = (Button)findViewById(R.id.buttonClose);
        buttonReset = (Button)findViewById(R.id.buttonReset);
        buttonWriteId = (Button)findViewById(R.id.buttonWriteId);
        progressBar = (TextView)findViewById(R.id.progressBar);
        idBar = (TextView)findViewById(R.id.idBar);
        imageView1 = (ImageView)findViewById(R.id.imageView1);
        editTextId = (EditText)findViewById(R.id.editTest1);

        progressBar.setText("");
        idBar.setText("");
        editTextId.setInputType(InputType.TYPE_CLASS_PHONE);

        buttonStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ble != null) {
                    return;
                }
                ble = new BluetoothLE(mainActivity, "ket_004");
                //ble = new BluetoothLE(mainActivity, et_device.getText().toString());
                ble.bleConnect();
                //testOpencv();
            }

        });
        
        buttonSend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ble != null) {
                    ble.bleWriteState((byte) 0x06);
                    buttonSend.setEnabled(false);
                    clearProcesssRate();
                    imageView1.setImageDrawable(null);
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
                    ble.bleWriteState((byte)0x00);
                    buttonSend.setEnabled(true);
                    clearProcesssRate();
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
                    byte [] bytes = new byte[5];
                    int temp = inputNum;
                    for(int i = 0; i < 5; i++){
                        bytes[4-i] = (byte) ((temp % 256) & 0xFF);
                        temp /= 256;
                    }
                    ble.bleWriteId(bytes);
                }
            }
        });

        buttonSend.setEnabled(false);
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
        buttonStart.setEnabled(false);
        buttonSend.setEnabled(true);
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
        buttonSend.setEnabled(false);
        buttonClose.setEnabled(false);
        buttonReset.setEnabled(false);
        buttonWriteId.setEnabled(false);
        idBar.setText("");
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
    public void updateProcessRate(float rate) {
        progressBar.setText(String.valueOf(rate).concat(" %"));
    }

    @Override
    public void clearProcesssRate() {
        progressBar.setText("");
    }

    @Override
    public void showImgPreview(String filePath) {
        Mat matOrigin = Imgcodecs.imread(filePath);
//        Mat matROI = matOrigin.submat(60, 160, 80, 240);
//
//        matOrigin.release();
//        Mat matClone = new Mat(matROI.cols(),matROI.rows(), CvType.CV_8UC1);
//        Imgproc.cvtColor(matROI, matClone, Imgproc.COLOR_RGB2GRAY);
//
//        Mat matFilter = new Mat(matClone.cols(), matClone.rows(), CvType.CV_8UC3);
//
//        Mat kernel = new Mat(8, 8, CvType.CV_32F);
//        kernel.setTo(new Scalar((double)1/64));
//        Imgproc.filter2D(matROI, matFilter, -1, kernel);
//
//        kernel.release();
//        matROI.release();
//        Mat matCanny = new Mat(matClone.cols(), matClone.rows(), CvType.CV_8UC1);
//        Imgproc.Canny(matFilter, matCanny, 20, 120, 3, true);
//
//        matFilter.release();
//        Mat matLines = new Mat();
//
//        int threshold = 20;
//        int minLineSize = 10;
//        int lineGap = 10;
//        Imgproc.HoughLinesP(matCanny, matLines, 1, Math.PI/180, threshold, lineGap , minLineSize);
//
//        matCanny.release();
//        Log.i(TAG, "Num of lines: " + matLines.rows());
//
//        int xmin = 160;
//        int xmax = 0;
//        int ymin = 100;
//        int ymax = 0;
//
//        for (int x = 0; x < matLines.rows(); x++)
//        {
//            double[] vec = matLines.get(x, 0);
//            double x1 = vec[0],
//                    y1 = vec[1],
//                    x2 = vec[2],
//                    y2 = vec[3];
//
//            if( xmin > (int) Math.min(x1, x2))
//                xmin = (int) Math.min(x1, x2);
//            if( xmax < (int) Math.max(x1, x2))
//                xmax = (int) Math.max(x1, x2);
//            if( ymin > (int) Math.min(y1, y2))
//                ymin = (int) Math.min(y1, y2);
//            if( ymax < (int) Math.max(y1, y2))
//                ymax = (int) Math.max(y1, y2);
//
//            //Point start = new Point(x1, y1);
//            //Point end = new Point(x2, y2);
//            //Imgproc.line(matClone, start, end, new Scalar(255,0,0), 3);
//        }
//
//        Log.i(TAG, "Xmin: "+ xmin + ", Xmax: " + xmax + ", Ymin: " + ymin + ", Ymax: " + ymax);
//        for(int i = 0; i < 2; i++) {
//            if (ymax - ymin > 25 && ymax - ymin < 35) {
//                if (xmax - xmin < 100) {
//                    if (xmin > 80)
//                        xmin = xmax - 100;
//                    else if (xmax < 80)
//                        xmax = xmin + 100;
//                    else {
//                    }
//                }
//            }
//
//            if (xmax - xmin > 70) {
//                if (ymax - ymin < 25) {
//                    if (ymin > 50)
//                        ymin = ymax - 30;
//                    else if (ymax < 50)
//                        ymax = ymin + 30;
//                    else {
//                    }
//                } else if (ymax - ymin > 35) {
//                    if (Math.abs(ymin) < Math.abs(240 - ymax))
//                        ymin = ymax - 30;
//                    else {
//                        ymax = ymin + 30;
//                    }
//                } else {
//                }
//            }
//        }
//        System.out.println("Xmin: "+ xmin + ", Xmax: " + xmax + ", Ymin: " + ymin + ", Ymax: " + ymax);
//        if( ymax-ymin < 0 || xmax-xmin < 0){
//           /* Handle exceptions*/
//            xmin = 36; xmax = 125; ymin = 30; ymax = 60;
//        }
//
//        //Imgproc.circle(matClone, new Point(xmin, ymin), 3, new Scalar(0, 255, 0), 3);
//        //Imgproc.circle(matClone, new Point(xmin, ymax), 3, new Scalar(0, 255, 0), 3);
//        //Imgproc.circle(matClone, new Point(xmax, ymin), 3, new Scalar(0, 255, 0), 3);
//        //Imgproc.circle(matClone, new Point(xmax, ymax), 3, new Scalar(0, 255, 0), 3);
//
//
//        matROI = matClone.submat(ymin+2, ymax-2, xmin+2, xmax-2);
//        Bitmap bmp = Bitmap.createBitmap(matROI.cols(), matROI.rows(), Bitmap.Config.ARGB_4444);
//        Utils.matToBitmap(matROI, bmp);
//
//        boolean result = checkResult(bmp);
//
//        Log.i( TAG, "Result: " + result);

        Bitmap bmp = Bitmap.createBitmap(matOrigin.cols(), matOrigin.rows(), Bitmap.Config.ARGB_4444);
        Utils.matToBitmap(matOrigin, bmp);
        imageView1.setImageBitmap(bmp);
    }

    @Override
    public void displayCurrentId(String id) {
        idBar.setText("Saliva Id: " + id);
    }

    @Override
	public void bleElectrodeAdcReading(byte state, byte[] adcReading) {
		// TODO Auto-generated method stub
	}

    public boolean checkResult(Bitmap image){
        int w = image.getWidth();
        int h = image.getHeight();
        Log.i(TAG, "width: " + w + " , height: " + h);

        final float eps = (float) -0.000001;
        float [] x0 = new float[w];
        float [] diff = new float[w-1];
        float [] pivot = new float[w-2];
        int [] rowNum = new int[]{6, 13, 18};
        float check = 0;

        for(int i = 0; i < h; i++){
            float maximum = 0;
            float minimum = 255;
            float sum = 0;
            Vector vector = new Vector();
            for (int j = 0; j < w; j++) {
                //int pixel = image.getRGB(j, rowNum[i]);
                int pixel = image.getPixel(j, i);
                int value = 255 - ((pixel >> 16) & 0xff);
                x0[j] = value;
                sum += x0[j];

                if( j > 0 ){
                    diff[j-1] = x0[j] - x0[j-1];
                    if (diff[j-1] == 0)
                        diff[j-1] = eps;
                }

                if( j > 1 ){
                    pivot[j-2] = diff[j-2] * diff[j-1];
                    if( pivot[j-2] < 0 && diff[j-2] > 0 ){
                        vector.add(j-1);
                    }
                }

                if(x0[j] > maximum)
                    maximum = x0[j];

                if(x0[j] < minimum)
                    minimum = x0[j];

            }
            if( (maximum - minimum) < 50 )
                continue;

//            Log.i(TAG, "Vector size: " + vector.size());

            float average = sum/w;
            float sel = (maximum-minimum)/4;
            boolean isFoundMax = false;
            int maxIdx = 0;
//            Log.i(TAG, "Sel: " + String.valueOf(sel));
            for(int k= 0; k < vector.size(); k++){
                int idx = (Integer) vector.get(k);
                if(x0[idx] == maximum){
//                    Log.i(TAG, "Maximum in Id:" + idx);
                    isFoundMax = true;
                    maxIdx = idx;
                    if(k == vector.size()-1){
                        check -= 1;
                    }
                }
                else if(isFoundMax == true) {
                    if(x0[idx] - average > sel){
//                        Log.i(TAG, String.valueOf((int) vector.get(k)));
                        if(idx > 5 && idx < w-6){
                            if( (idx - maxIdx) > 35 && (idx - maxIdx) < 45){
                                if(x0[idx] - x0[idx-5] > sel/3 && x0[idx] - x0[idx+5] > sel/3){
                                    //System.out.println(idx);
                                    check += 5;
                                }
                                else{
                                    check -= 1;
                                }
                                break;
                            }
                        }
                    }
                    else if(k == vector.size()-1){
                        check -= 1;
                    }
                }
            }
        }
        Log.i(TAG, "Check: " + String.valueOf(check));
        if(check > 0)
            return true;
        else
            return false;
    }

    public void testOpencv() {
        Log.i(TAG, "Test opencv.");
        File mainStorage = null;
        if (mainStorage == null) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                mainStorage = new File(Environment.getExternalStorageDirectory(), "TempPicDir");
            else
                mainStorage = new File(activity.getApplicationContext().getFilesDir(), "TempPicDir");
        }
        if (!mainStorage.exists())
            mainStorage.mkdirs();

        File file = new File(mainStorage, "sample5.jpg");

        Mat matOrigin = Imgcodecs.imread(file.getAbsolutePath());
        Mat matROI = matOrigin.submat(60, 160, 80, 240);

        matOrigin.release();
        Mat matClone = new Mat(matROI.cols(),matROI.rows(), CvType.CV_8UC1);
        Imgproc.cvtColor(matROI, matClone, Imgproc.COLOR_RGB2GRAY);

        Mat matFilter = new Mat(matClone.cols(), matClone.rows(), CvType.CV_8UC3);

        Mat kernel = new Mat(10, 10, CvType.CV_32F);
        kernel.setTo(new Scalar((double)1/100));
//        for(int i = 0; i < 8; i++){
//            for(int j = 0; j < 8; j++){
//                kernel.put(i, j, (float) 1/64);
//            }
//        }
//        Log.i(TAG, matClone.toString());
//        Log.i(TAG, matClone.dump());
        Imgproc.filter2D(matClone, matFilter, -1, kernel);

        kernel.release();
        matROI.release();
        Mat matCanny = new Mat(matClone.cols(), matClone.rows(), CvType.CV_8UC1);
        Imgproc.Canny(matFilter, matCanny, 20, 100, 3, true);
        matFilter.release();
        Mat matLines = new Mat();

        int threshold = 20;
        int minLineSize = 10;
        int lineGap = 10;
        Imgproc.HoughLinesP(matCanny, matLines, 1, Math.PI/180, threshold, lineGap , minLineSize);

        matCanny.release();
        Log.i(TAG, "Num of lines: " + matLines.cols());   // Warning: The number of lines is different from java version.
//        Log.i(TAG, "Cols: " + matLines.cols());
//        Log.i(TAG, "Dump: " + matLines.dump());

        int xmin = 160;
        int xmax = 0;
        int ymin = 100;
        int ymax = 0;

        for (int x = 0; x < matLines.cols(); x++)
        {
            double[] vec = matLines.get(0, x);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];

//            Log.i(TAG, "x1: "+ x1 + ", x2: " + x2 + ", y1: " + y1 + ", y2: " + y2);

            if( xmin > (int) Math.min(x1, x2))
                xmin = (int) Math.min(x1, x2);
            if( xmax < (int) Math.max(x1, x2))
                xmax = (int) Math.max(x1, x2);
            if( ymin > (int) Math.min(y1, y2))
                ymin = (int) Math.min(y1, y2);
            if( ymax < (int) Math.max(y1, y2))
                ymax = (int) Math.max(y1, y2);

//            Point start = new Point(x1, y1);
//            Point end = new Point(x2, y2);
//            Imgproc.line(matClone, start, end, new Scalar(255,0,0), 3);
        }

        Log.i(TAG, "Xmin: "+ xmin + ", Xmax: " + xmax + ", Ymin: " + ymin + ", Ymax: " + ymax);
        for(int i = 0; i < 2; i++) {
            if (ymax - ymin > 25 && ymax - ymin < 35) {
                if (xmax - xmin < 80) {
                    if (xmin > 55)
                        xmin = xmax - 90;
                    else if (xmax < 110)
                        xmax = xmin + 90;
                    else {
                    }
                }
                else if(xmax - xmin > 100){
                    if(Math.abs(xmin) < Math.abs(160-xmax))
                        xmin = xmax - 90;
                    else{
                        xmax = xmin + 90;
                    }
                }
                else{
                }
            }

            if (xmax - xmin > 70) {
                if (ymax - ymin < 25) {
                    if (ymin > 50)
                        ymin = ymax - 30;
                    else if (ymax < 50)
                        ymax = ymin + 30;
                    else {
                    }
                } else if (ymax - ymin > 35) {
                    if (Math.abs(ymin) < Math.abs(100 - ymax))
                        ymin = ymax - 30;
                    else {
                        ymax = ymin + 30;
                    }
                }
                else{
                }
            }
        }
        Log.i(TAG, "Xmin: " + xmin + ", Xmax: " + xmax + ", Ymin: " + ymin + ", Ymax: " + ymax);
        if( ymax-ymin < 25 || xmax-xmin < 50){
           /* Handle exceptions*/
            xmin = 36; xmax = 125; ymin = 30; ymax = 60;
        }

//        Imgproc.circle(matClone, new Point(xmin, ymin), 3, new Scalar(0, 255, 0), 3);
//        Imgproc.circle(matClone, new Point(xmin, ymax), 3, new Scalar(0, 255, 0), 3);
//        Imgproc.circle(matClone, new Point(xmax, ymin), 3, new Scalar(0, 255, 0), 3);
//        Imgproc.circle(matClone, new Point(xmax, ymax), 3, new Scalar(0, 255, 0), 3);

        matROI = matClone.submat(ymin+3, ymax-3, xmin+3, xmax-3);
        Bitmap bmp = Bitmap.createBitmap(matROI.cols(), matROI.rows(), Bitmap.Config.ARGB_4444);
        Utils.matToBitmap(matROI, bmp);

        boolean result = checkResult(bmp);

        Log.i( TAG, "Result: " + result);

        imageView1.setImageBitmap(bmp);
    }

}
