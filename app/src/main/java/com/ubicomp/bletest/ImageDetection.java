package com.ubicomp.bletest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created by larry on 15/7/22.
 */
public class ImageDetection {
    private static final String TAG = "BluetoothLE";

    private static final int ROI_X_MIN = 80;
    private static final int ROI_X_MAX = 240;
    private static final int ROI_Y_MIN = 80;
    private static final int ROI_Y_MAX = 160;

    private static final int DEFAULT_X_MIN = 50;
    private static final int DEFAULT_X_MAX = 140;
    private static final int DEFAULT_Y_MIN = 20;
    private static final int DEFAULT_Y_MAX = 50;

    private static final int WHITE_THRESHOLD = 180;
    private static final int VALID_THRESHOLD = -15;
    private static final int MINIMAL_EFFECTIVE_RANGE = 20;

    private static final int LBOUND_BETWEEN_LINE = 30;
    private static final int UBOUND_BETWEEN_LINE = 45;

    private static final int LBOUND_EFFECTIVE_GRAYSCALE = 40;
    private static final float NO_LINE_PENALTY = 1;

    private static final int LBOUND_FIRST_LINE_RANGE = 20;
    private static final int UBOUND_FIRST_LINE_RANGE = 40;
    private static final int SELECTIVITY_CONST = 3;

    private static final int FIRST_LINE_UNFOUND_PENALTY = 1;
    private static final int SECOND_LINE_UNFOUND_PENALTY = 1;
    private static final int FOUND_REWARD = 4;
    private static final int ALLOWED_TEST_LINE_WIDTH = 2;

    private static final int CHECK_BOUNDARY = 0;

    private static final float eps = (float) -0.000001;

    private Activity activity = null;
    private DataTransmission datatransmission = null;
    private String debugURL = null;

    private int xmin = DEFAULT_X_MIN;
    private int xmax = DEFAULT_X_MAX;
    private int ymin = DEFAULT_Y_MIN;
    private int ymax = DEFAULT_Y_MAX;

    public ImageDetection(Activity activity, String url) {

        this.activity = activity;
        this.debugURL = url;
    }

    public ImageDetection(Activity activity, DataTransmission datatransmission) {
        this.activity = activity;
        this.datatransmission = datatransmission;
    }

    public void roiDetectionOnWhite(Bitmap bitmap) {
        Mat matOrigin = new Mat();
        Utils.bitmapToMat(bitmap, matOrigin);

        Mat matROI = matOrigin.submat(ROI_Y_MIN, ROI_Y_MAX, ROI_X_MIN, ROI_X_MAX);

        Mat matClone = new Mat(matROI.cols(),matROI.rows(), CvType.CV_8UC1);
        Imgproc.cvtColor(matROI, matClone, Imgproc.COLOR_RGB2GRAY);

        Bitmap roiBmp = Bitmap.createBitmap(matROI.cols(), matROI.rows(), Bitmap.Config.ARGB_4444);
        Utils.matToBitmap(matROI, roiBmp);

//        Bitmap roiBmp = Bitmap.createBitmap(bitmap, ROI_X_MIN, ROI_Y_MIN, ROI_X_MAX - ROI_X_MIN, ROI_Y_MAX - ROI_Y_MIN);

        int width = roiBmp.getWidth();
        int height = roiBmp.getHeight();

        int xSum = 0;
        int ySum = 0;
        int count = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = roiBmp.getPixel(j, i);
                int value = ((pixel >> 16) & 0xff);
                if (value > WHITE_THRESHOLD) {
                    xSum += j*value;
                    ySum += i*value;
                    count += value;
                }
            }
        }

        int xCenter = xSum / count;
        int yCenter = ySum / count;

        xmin = xCenter - 45;
        xmax = xCenter + 45;
        ymin = yCenter - 15;
        ymax = yCenter + 15;

        Log.i(TAG, "xmin: " + xmin + ", xmax: " + xmax + ", ymin: " + ymin + ", ymax: " + ymax);

        Point p1 = new Point(ROI_X_MIN + xmin, ROI_Y_MIN + ymin);
        Point p2 = new Point(ROI_X_MIN + xmin, ROI_Y_MIN + ymax);
        Point p3 = new Point(ROI_X_MIN + xmax, ROI_Y_MIN + ymin);
        Point p4 = new Point(ROI_X_MIN + xmax, ROI_Y_MIN + ymax);

        Imgproc.line(matOrigin, p1, p2, new Scalar(0, 0, 0), 3);
        Imgproc.line(matOrigin, p2, p4, new Scalar(0, 0, 0), 3);
        Imgproc.line(matOrigin, p4, p3, new Scalar(0, 0, 0), 3);
        Imgproc.line(matOrigin, p3, p1, new Scalar(0, 0, 0), 3);


        Bitmap bmp = Bitmap.createBitmap(matOrigin.cols(), matOrigin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matOrigin, bmp);


        String roiPath = null;
        if (datatransmission != null){
            String picturePath = datatransmission.file.getAbsolutePath();
            roiPath = picturePath.substring(0, picturePath.lastIndexOf(".")).concat("_1.jpg");
        }
        else{
            roiPath = debugURL.substring(0, debugURL.lastIndexOf(".")).concat("_8.jpg");
        }

        File file = new File(roiPath); // the File to save to
        FileOutputStream fout = null;

        try {
            fout = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 50, fout);
            Bundle countBundle = new Bundle();
            countBundle.putString("picturePath", file.getAbsolutePath());

            Message msg = new Message();
            msg.what = MainActivity.PICTURE_PREVIEW_MSG;
            msg.setData(countBundle);
            ((MainActivity) activity).mHandler.sendMessage(msg);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    public boolean testStripDetection(Bitmap bitmap) {

        Bitmap roiBmp = Bitmap.createBitmap(bitmap, ROI_X_MIN + xmin + 2, ROI_Y_MIN + ymin + 1, xmax - xmin - 4, ymax - ymin - 2);
        int width = roiBmp.getWidth();
        int height = roiBmp.getHeight();
        int middle = width / 2;
        int halfHeight = height / 2;
        Log.i(TAG, "width: " + width + " , height: " + height);

        Mat matROI = new Mat();
        Utils.bitmapToMat(roiBmp, matROI);

        float[] x0 = new float[width];
        float[] diff = new float[width - 1];
        float[] pivot = new float[width - 2];
        float validity = 0;
        float check = 0;
        float overallSum = 0;
        float overallAvg = 0;

        HashMap testLineVoteMap = new HashMap();

        for (int i = 0; i < height; i++) {
            float maximum = 0;
            float minimum = 255;
            float sumAll = 0;
            float sumAfterMiddle = 0;
            float maximumAfterMiddle = 0;
            float minimumAfterMiddle = 255;
            Vector vector = new Vector();
            for (int j = 0; j < width; j++) {
                //int pixel = image.getRGB(j, i);
                int pixel = roiBmp.getPixel(j, i);
                int value = 255 - ((pixel >> 16) & 0xff);
                x0[j] = value;
                sumAll += x0[j];
                if (j >= middle) {
                    sumAfterMiddle += x0[j];
                }

                if (j > 0) {
                    diff[j - 1] = x0[j] - x0[j - 1];
                    if (diff[j - 1] == 0)
                        diff[j - 1] = eps;
                }

                if (j > 1) {
                    pivot[j - 2] = diff[j - 2] * diff[j - 1];
                    if (pivot[j - 2] < 0 && diff[j - 2] > 0) {
                        vector.add(j - 1);
                    }
                }

                if (x0[j] > maximum)
                    maximum = x0[j];

                if (x0[j] < minimum)
                    minimum = x0[j];

                if (j >= middle) {
                    if (x0[j] > maximumAfterMiddle)
                        maximumAfterMiddle = x0[j];

                    if (x0[j] < minimumAfterMiddle)
                        minimumAfterMiddle = x0[j];
                }

            }
            float avgAll = sumAll / width;
            if( i > height/4 && i < 3*height/4 ){
                overallSum += avgAll;
            }

            if ((maximum - minimum) < MINIMAL_EFFECTIVE_RANGE) {
                Log.i(TAG, "Useless row.");
                if (avgAll > LBOUND_EFFECTIVE_GRAYSCALE) {
                    validity -= NO_LINE_PENALTY;
                    //check -= 0.5;                               // Modified by larry on 7/27
                }
                continue;
            }


            float avgAfterMiddle = sumAfterMiddle / middle;
            float sel = (maximum - minimum) / SELECTIVITY_CONST;
            float selAfterMiddle;

            selAfterMiddle = (maximumAfterMiddle-minimumAfterMiddle)/(SELECTIVITY_CONST+10);


            float refCandidate = 0;
            boolean isFoundRef = false;
            int refIdx = 0;
            float secondMaximal = 0;
            int secondIdx = 0;

            Vector candidateVector = new Vector();
//            Log.i(TAG, "K = " + vector.size());
            for (int k = 0; k < vector.size(); k++) {
                int idx = (int) vector.get(k);

                if (idx > LBOUND_FIRST_LINE_RANGE && idx <= UBOUND_FIRST_LINE_RANGE) {
                    if (x0[idx] - avgAll > sel) {
                        candidateVector.add(idx);
                        Log.i(TAG, "Reference in Id:" + idx);
                    }
                }

                if (idx > UBOUND_FIRST_LINE_RANGE && isFoundRef == false) {
                    for (int m = 0; m < candidateVector.size(); m++) {
                        int tempIdx = (int) candidateVector.get(m);
                        if (x0[tempIdx] > refCandidate) {
                            refCandidate = x0[tempIdx];
                            refIdx = tempIdx;
                        }
                    }
                    if (refIdx == 0) {
                        Log.i(TAG, "Can't find refPoint in " + i + "th row.");
                        validity -= FIRST_LINE_UNFOUND_PENALTY;
                        break;
                    } else {
                        Point point = new Point(refIdx, i);
                        Imgproc.circle(matROI, point, 1, new Scalar(0, 255, 0), 1);
                        isFoundRef = true;
                    }
                }

                if (idx > middle && isFoundRef == true) {
                    if (x0[idx] - avgAfterMiddle > selAfterMiddle) {
                        if (secondMaximal < x0[idx]) {
                            secondMaximal = x0[idx];
                            secondIdx = idx;
                        }
                    }
                }

                if (k == vector.size() - 1) {
                    if (secondIdx != 0 && (secondIdx - refIdx) > LBOUND_BETWEEN_LINE && (secondIdx - refIdx) < UBOUND_BETWEEN_LINE) {
                        Log.i(TAG, "Second: " + secondIdx);

                        int value = 1;
                        if(testLineVoteMap.containsKey(secondIdx) == true){
                            value = (int) testLineVoteMap.get(secondIdx);
                            value++;
                        }
                        testLineVoteMap.put(secondIdx, value);

                        Point point = new Point(secondIdx, i);
                        Imgproc.circle(matROI, point, 1, new Scalar(0, 0, 255), 1);
                        //check += FOUND_REWARD;
                    } else {
                        Log.i(TAG, "Failed");
                        check -= SECOND_LINE_UNFOUND_PENALTY;
                    }
                    break;
                }
            }
        }

        overallAvg = overallSum / (height/2);
        Bundle countBundle = new Bundle();
        countBundle.putFloat("average", overallAvg);
        Message msg = new Message();
        msg.what = MainActivity.SHOW_AVG_GRAY_VALUE;
        msg.setData(countBundle);
        ((MainActivity) activity).mHandler.sendMessage(msg);

        if (!testLineVoteMap.isEmpty()){

            int maxSecondIdx = 0;
            int tempMaxSecondIdxNum = 0;
            for (Object key : testLineVoteMap.keySet()) {
                int value = (int)testLineVoteMap.get(key);
                if( value > tempMaxSecondIdxNum){
                    tempMaxSecondIdxNum = value;
                    maxSecondIdx = (int) key;
                }
                Log.i(TAG, key + " : " + testLineVoteMap.get(key));
            }

            for (Object key : testLineVoteMap.keySet()) {
                int candidateIdx = (int) key;
                if( Math.abs(candidateIdx - maxSecondIdx) <= ALLOWED_TEST_LINE_WIDTH) {
                    int value = (int) testLineVoteMap.get(key);
                    check += (value * FOUND_REWARD);
                }
            }
        }

        Log.i(TAG, "Validatity: " + String.valueOf(validity));
        if (validity < VALID_THRESHOLD)
            check = -1000;

        Log.i(TAG, "Check: " + String.valueOf(check));
        countBundle = new Bundle();
        countBundle.putFloat("check", check);
        msg = new Message();
        msg.what = MainActivity.SHOW_PREDICTION_MSG;
        msg.setData(countBundle);
        ((MainActivity) activity).mHandler.sendMessage(msg);


        String roiPath = null;
        if (datatransmission != null){
            String picturePath = datatransmission.file.getAbsolutePath();
            roiPath = picturePath.substring(0, picturePath.lastIndexOf(".")).concat("_2.jpg");
        }
        else{
            roiPath = debugURL.substring(0, debugURL.lastIndexOf(".")).concat("_9.jpg");
        }

        File file = new File(roiPath); // the File to save to
        FileOutputStream fout = null;

        try {
            fout = new FileOutputStream(file);
            Bitmap labelBmp = Bitmap.createBitmap(matROI.cols(), matROI.rows(), Bitmap.Config.ARGB_4444);
            Utils.matToBitmap(matROI, labelBmp);
            labelBmp.compress(Bitmap.CompressFormat.JPEG, 50, fout);
            countBundle = new Bundle();
            countBundle.putString("picturePath", file.getAbsolutePath());

            msg = new Message();
            msg.what = MainActivity.PICTURE_PREVIEW_MSG;
            msg.setData(countBundle);
            ((MainActivity) activity).mHandler.sendMessage(msg);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if (check > CHECK_BOUNDARY)
            return true;
        else
            return false;

    }

}