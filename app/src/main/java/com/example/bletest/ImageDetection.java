package com.example.bletest;

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
import java.util.Vector;

/**
 * Created by larry on 15/7/22.
 */
public class ImageDetection {
    private static final String TAG = "BluetoothLE";

    private static final int ROI_X_MIN = 80;
    private static final int ROI_X_MAX = 240;
    private static final int ROI_Y_MIN = 60;
    private static final int ROI_Y_MAX = 160;

    private static final int DEFAULT_X_MIN = 45;
    private static final int DEFAULT_X_MAX = 135;
    private static final int DEFAULT_Y_MIN = 20;
    private static final int DEFAULT_Y_MAX = 50;

    private static final int whiteThreshold = 230;
    private static final int minimalRange = 50;

    private Activity activity = null;
    private DataTransmission datatransmission = null;

    private int xmin = ROI_X_MIN;
    private int xmax = ROI_X_MAX;
    private int ymin = ROI_Y_MIN;
    private int ymax = ROI_Y_MAX;


    public ImageDetection(Activity activity, DataTransmission datatransmission){
        this.activity = activity;
        this.datatransmission = datatransmission;
    }

    public void roiDetectionOnWhite(Bitmap bitmap){
        Mat matOrigin = new Mat();
        Utils.bitmapToMat(bitmap, matOrigin);
        Mat matROI = matOrigin.submat(ROI_Y_MIN, ROI_Y_MAX, ROI_X_MIN, ROI_X_MAX);

        //Mat matClone = new Mat(matROI.cols(),matROI.rows(), CvType.CV_8UC1);
        //Imgproc.cvtColor(matROI, matClone, Imgproc.COLOR_RGB2GRAY);

        Bitmap roiBmp = Bitmap.createBitmap(matROI.cols(), matROI.rows(), Bitmap.Config.ARGB_4444);
        Utils.matToBitmap(matROI, roiBmp);

        int w = roiBmp.getWidth();
        int h = roiBmp.getHeight();

        int xSum = 0;
        int ySum = 0;
        int count = 0;

        for(int i = 0; i < h; i++){
            for (int j = 0; j < w; j++) {
                int pixel = roiBmp.getPixel(j, i);
                int value = ((pixel >> 16) & 0xff);
                if (value > whiteThreshold) {
                    xSum += j;
                    ySum += i;
                    count++;
                }
            }
        }

        int xCenter = xSum / count;
        int yCenter = ySum / count;

        xmin = xCenter - 45;
        xmax = xCenter + 45;
        ymin = yCenter - 15;
        ymax = yCenter + 25;

        Log.i(TAG, "xmin: "+ xmin + ", xmax: " + xmax + ", ymin: " + ymin + ", ymax: " + ymax);

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

        String picturePath = datatransmission.file.getAbsolutePath();
        String roiPath = picturePath.substring(0, picturePath.lastIndexOf(".")).concat("_1.jpg");

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


    public boolean testStripDetection(Bitmap bitmap){
        Bitmap roiBmp = Bitmap.createBitmap(bitmap, ROI_X_MIN + xmin + 3, ROI_Y_MIN + ymin + 3, xmax - xmin - 6, ymax -ymin-6);
        int w = roiBmp.getWidth();
        int h = roiBmp.getHeight();
        Log.i(TAG, "width: " + w + " , height: " + h);

        final float eps = (float) -0.000001;
        float [] x0 = new float[w];
        float [] diff = new float[w-1];
        float [] pivot = new float[w-2];
        float check = 0;

        for(int i = 0; i < h; i++){
            float maximum = 0;
            float minimum = 255;
            float sumAll= 0;
            float sumAfter50 = 0;
            float maximumAfter50 = 0;
            float minimumAfter50 = 255;
            Vector vector = new Vector();
            for (int j = 0; j < w; j++) {
                //int pixel = image.getRGB(j, i);
                int pixel = roiBmp.getPixel(j, i);
                int value = 255 - ((pixel >> 16) & 0xff);
                x0[j] = value;
                sumAll += x0[j];
                if(j >= 50){
                    sumAfter50 += x0[j];
                }

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

                if(j >= 50){
                    if(x0[j] > maximumAfter50)
                        maximumAfter50 = x0[j];

                    if(x0[j] < minimumAfter50)
                        minimumAfter50 = x0[j];
                }

            }
            float avgAll = sumAll /w;
            if( (maximum - minimum) < minimalRange ){
                Log.i(TAG, "Useless row.");
                if( avgAll > 50) {
                    check -= 0.5;                               // MOdified by larry on 7/27
                }
                continue;
            }



            float avgAfter50 = sumAfter50 /(w-50);
            float sel = (maximum-minimum)/5;
            float selAfter50 = (maximumAfter50-minimumAfter50)/5;
            float refCandidate = 0;
            boolean isFoundRef = false;
            int refIdx = 0;
            float secondMaximal = 0;
            int secondIdx = 0;

//            Log.i(TAG, "Avg: " + String.valueOf(avgAll));
//            Log.i(TAG, "AvgAfter50: " + String.valueOf(avgAfter50));
//            Log.i(TAG, "Sel: " + String.valueOf(sel));
//            Log.i(TAG, "SelAfter50: " + String.valueOf(selAfter50));

            if(vector.size() <= 1){
                Log.i(TAG, "Can't find refPoint in " + i + "th row.");
                check -= 1;
                continue;
            }

            Vector candidateVector = new Vector();
//            Log.i(TAG, "K = " + vector.size());
            for(int k= 0; k < vector.size(); k++) {
                int idx = (int) vector.get(k);

                if (idx > 20 && idx <= 40) {
                    if (x0[idx] - avgAll > sel) {
                        candidateVector.add(idx);
                        Log.i(TAG, "Reference in Id:" + idx);
                    }
                } else if (idx > 40 && isFoundRef == false) {
                    for (int m = 0; m < candidateVector.size(); m++) {
                        int tempIdx = (int) candidateVector.get(m);
                        if (x0[tempIdx] > refCandidate) {
                            refCandidate = x0[tempIdx];
                            refIdx = tempIdx;
                        }
                    }
                    if (refIdx == 0) {
                        Log.i(TAG, "Can't find refPoint in " + i + "th row.");
                        check -= 1;
                        break;
                    }
                    isFoundRef = true;
                } else if (idx > 50 && isFoundRef == true) {
                    if (x0[idx] - avgAfter50 > selAfter50) {
                        if (secondMaximal < x0[idx]) {
                            secondMaximal = x0[idx];
                            secondIdx = idx;
                        }
                    }
                }
                else{

                }

                if (k == vector.size() - 1) {
                    if (secondIdx != 0 && (secondIdx - refIdx) > 30 && (secondIdx - refIdx) < 45) {
                        Log.i(TAG, "Second: " + secondIdx);
                        check += 4;
                    } else {
                        Log.i(TAG, "Failed" );
                        check -= 1;
                    }
                    break;
                }
            }
        }

        Log.i(TAG, "Check: " + String.valueOf(check));
        Bundle countBundle = new Bundle();
        countBundle.putFloat("check", check);

        Message msg = new Message();
        msg.what = MainActivity.SHOW_PREDICTION_MSG;
        msg.setData(countBundle);
        ((MainActivity) activity).mHandler.sendMessage(msg);

        if(check > 0)
            return true;
        else
            return false;

    }

    /* Obsolete functions*/
    //    public boolean roiDetection(Bitmap bitmap){
//
//        boolean result = false;
//        Mat matOrigin = new Mat ();
//        Utils.bitmapToMat(bitmap, matOrigin);
//
//        //Mat matOrigin = Imgcodecs.imread(filePath);
//        Mat matROI = matOrigin.submat(ROI_Y_MIN, ROI_Y_MAX, ROI_X_MIN, ROI_X_MAX);
//
//        //matOrigin.release();
//        Mat matClone = new Mat(matROI.cols(),matROI.rows(), CvType.CV_8UC1);
//        Imgproc.cvtColor(matROI, matClone, Imgproc.COLOR_RGB2GRAY);
//
//        Mat matFilter = new Mat(matClone.cols(), matClone.rows(), CvType.CV_8UC3);
//
//        int filterSize = 8;
//
//        Mat kernel = new Mat(filterSize, filterSize, CvType.CV_32F);
//        kernel.setTo(new Scalar((double)1 /(filterSize * filterSize)));
//
//        Imgproc.filter2D(matClone, matFilter, -1, kernel);
//
//        kernel.release();
//        matROI.release();
//        Mat matCanny = new Mat(matClone.cols(), matClone.rows(), CvType.CV_8UC1);
//        Imgproc.Canny(matFilter, matCanny, 20, 100, 3, true);
//        matFilter.release();
//        Mat matLines = new Mat();
//
//        int houghThreshold = 20;
//        int minLineSize = 10;
//        int lineGap = 10;
//        Imgproc.HoughLinesP(matCanny, matLines, 1, Math.PI/180, houghThreshold, lineGap , minLineSize);
//
//        matCanny.release();
//        Log.i(TAG, "Num of lines: " + matLines.cols());   // Warning: The number of lines is different from java version.
//
//        xmin = ROI_X_MAX-ROI_X_MIN;
//        xmax = 0;
//        ymin = ROI_Y_MAX-ROI_Y_MIN;
//        ymax = 0;
//
//        for (int x = 0; x < matLines.cols(); x++)
//        {
//            double[] vec = matLines.get(0, x);
//            double  x1 = vec[0],
//                    y1 = vec[1],
//                    x2 = vec[2],
//                    y2 = vec[3];
//
////            Log.i(TAG, "x1: "+ x1 + ", x2: " + x2 + ", y1: " + y1 + ", y2: " + y2);
//            if( xmin > (int) Math.min(x1, x2))
//                xmin = (int) Math.min(x1, x2);
//            if( xmax < (int) Math.max(x1, x2))
//                xmax = (int) Math.max(x1, x2);
//            if( ymin > (int) Math.min(y1, y2))
//                ymin = (int) Math.min(y1, y2);
//            if( ymax < (int) Math.max(y1, y2))
//                ymax = (int) Math.max(y1, y2);
//
//        }
//
//        Log.i(TAG, "xmin: "+ xmin + ", xmax: " + xmax + ", ymin: " + ymin + ", ymax: " + ymax);
//        for(int i = 0; i < 2; i++) {
//            if (ymax - ymin > 25 && ymax - ymin < 35) {
//                if (xmax - xmin < 80) {
//                    if (xmin > 55)
//                        xmin = xmax - 90;
//                    else if (xmax < 110)
//                        xmax = xmin + 90;
//                    else {
//                    }
//                }
//                else if(xmax - xmin > 100){
//                    if(Math.abs(xmin) < Math.abs(160-xmax))
//                        xmin = xmax - 90;
//                    else{
//                        xmax = xmin + 90;
//                    }
//                }
//                else{
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
//                    if (Math.abs(ymin) < Math.abs(100 - ymax))
//                        ymin = ymax - 30;
//                    else {
//                        ymax = ymin + 30;
//                    }
//                }
//                else{
//                }
//            }
//        }
//        Log.i(TAG, "xmin: " + xmin + ", xmax: " + xmax + ", ymin: " + ymin + ", ymax: " + ymax);
//        if( ymax-ymin < 25 || xmax-xmin < 50){
//           /* Handle exceptions*/
//            xmin = DEFAULT_X_MIN; xmax = DEFAULT_X_MAX; ymin = DEFAULT_Y_MIN; ymax = DEFAULT_Y_MAX;
//        }
//        else{
//            result = true;
//        }
//
//        Point p1 = new Point(ROI_X_MIN + xmin, ROI_Y_MIN + ymin);
//        Point p2 = new Point(ROI_X_MIN + xmin, ROI_Y_MIN + ymax);
//        Point p3 = new Point(ROI_X_MIN + xmax, ROI_Y_MIN + ymin);
//        Point p4 = new Point(ROI_X_MIN + xmax, ROI_Y_MIN + ymax);
//
//        Imgproc.line(matOrigin, p1, p2, new Scalar(255,0,0), 3);
//        Imgproc.line(matOrigin, p2, p4, new Scalar(255,0,0), 3);
//        Imgproc.line(matOrigin, p4, p3, new Scalar(255,0,0), 3);
//        Imgproc.line(matOrigin, p3, p1, new Scalar(255,0,0), 3);
//
////        matROI = matClone.submat(ymin + 3, ymax - 3, xmin+3, xmax-3);
////        Bitmap bmp = Bitmap.createBitmap(matROI.cols(), matROI.rows(), Bitmap.Config.ARGB_4444);
////        Utils.matToBitmap(matROI, bmp);
//
//        Bitmap bmp = Bitmap.createBitmap(matOrigin.cols(), matOrigin.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matOrigin, bmp);
//
//        ((BluetoothListener) activity).setImgPreview(bmp);
//        return result;
//    }
}
