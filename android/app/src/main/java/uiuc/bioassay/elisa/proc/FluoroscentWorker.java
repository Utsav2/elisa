package uiuc.bioassay.elisa.proc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.*;

import android.widget.TextView;


import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import uiuc.bioassay.elisa.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static org.bytedeco.javacpp.opencv_core.CV_16S;
import static org.bytedeco.javacpp.opencv_core.cvOpenFileStorage;
import static org.bytedeco.javacpp.opencv_core.minMaxLoc;
import static org.bytedeco.javacpp.opencv_imgproc.accumulate;

/**
 * Created by utsav on 11/11/2016.
 */

public class FluoroscentWorker extends AsyncTask<String, Void, Integer> {

    private Context mContext;
    private ProgressDialog progressDialog;
    private String folder;
    private static final String TAG = "FluoroscentWorker";

    public FluoroscentWorker(Context context) {
        mContext = context;
        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Wait");
        progressDialog.setMessage("Processing Video...");
        progressDialog.show();
    }

    @Override
    protected Integer doInBackground(String... params) {
        folder = params[0];
        String videoFile = params[1];
        // TODO(utsav) replace this with actual camera footage
        return startProcessingVideo(a("MOV_0051.MP4"));
    }

    private String a(String name) {
        File cacheFile = new File(mContext.getCacheDir(), name);
        try {
            InputStream inputStream = mContext.getAssets().open(name);
            try {
                FileOutputStream outputStream = new FileOutputStream(cacheFile);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not open robot png", e);
        }
        System.gc();
        return cacheFile.getAbsolutePath();
    }

    static class MatrixInfo {
        Mat m;
        final int index;
        double mean;
        public MatrixInfo(Mat m, int index, double mean) {
            this.m = m;
            this.index = index;
            this.mean = mean;
        }

        // when we don't need the mean
        public MatrixInfo(Mat m, int index) {
            this(m, index, -1);
        }
    }

    // used to sort matrices based on when we saw them
    static class MatrixIndexComparator implements Comparator<MatrixInfo> {
        @Override
        public int compare(MatrixInfo matrixInfo, MatrixInfo t1) {
            return matrixInfo.index - t1.index;
        }
    }

    static class MatrixMeanComparator implements Comparator<MatrixInfo> {

        @Override
        public int compare(MatrixInfo o1, MatrixInfo o2) {
            return (int) (o2.mean - o1.mean);
        }
    }

    private int startProcessingVideo(String videoFile) {
        FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(videoFile);
        videoGrabber.setFormat("mp4");
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        int MAX_COUNT = 8;
        List<MatrixInfo> mats = new ArrayList<>();
        // initial capacity, comparator
        PriorityQueue<MatrixInfo> q = new PriorityQueue<>(MAX_COUNT, new MatrixMeanComparator());
        try {
            videoGrabber.start();
        } catch (FrameGrabber.Exception e) {
            Log.e(TAG, "Exception in grabbing frames", e);
        }
        Frame vFrame = null;
        int count = 0;
        double max = -1;
        Range r1 = new Range(251, 1080);
        Range r2 = new Range(701, 1120);
        while(true) {
            try {
                vFrame = videoGrabber.grabImage();
                if (vFrame == null) {
                    break;
                }
                Mat main = converterToMat.convert(vFrame);
                Mat m  = main.apply(r1, r2);
                double mean = opencv_core.mean(m).magnitude();
                if (mean < 2) {
                    m._deallocate();
                    main._deallocate();
                    continue;
                }
                // this is a heavy operation, that for some reason even on deallocation
                // takes up memory (enough to force Android to kill the app -- after killing
                // other processes). I'm not sure if it's simply creation of the mat objects,
                // it's probably the conversion
                Mat mat = new Mat();
                m.convertTo(mat, CV_16S);
                // end heavy operation

                main._deallocate();
                mat._deallocate();
                m._deallocate();
                MatrixInfo t = new MatrixInfo(mat, count, mean);
                Log.d(TAG, "count: " + count + "mean: " + t.mean);
                mats.add(t);
                if (mean > max) {
                    max = mean;
                }
                q.add(t);
                count++;
            } catch (FrameGrabber.Exception e) {
                Log.e(TAG, "exception in retreiving a frame", e);
                break;
            }
        };

        double threshold = 0.2 * max;
        Log.d(TAG, "threshold: " + threshold);

        ArrayList<MatrixInfo> outs = new ArrayList<>();

        for (int i = 0; i < MAX_COUNT; i++) {
            List<Mat> framesToAverage = new ArrayList<Mat>();
            MatrixInfo t = q.remove();
            framesToAverage.add(t.m);
            // now remove from the sides of this peak until we reach threshold
            int leftIndex = t.index - 1;
            int rightIndex = t.index + 1;
            for(int j = leftIndex; j >= 0; j--) {
                MatrixInfo m = mats.get(j);
                if (m.mean < threshold) {
                    break;
                }
                q.remove(m);
                framesToAverage.add(m.m);
            }

            for (int j = rightIndex; j < count; j++) {
                MatrixInfo m = mats.get(j);
                if (m.mean < threshold) {
                    break;
                }
                q.remove(m);

                // imwrite("C:\\Users\\utsav\\Pictures\\images\\img" + i + "-debug-" + d + ".jpg", m.m);
                framesToAverage.add(m.m);
            }
            Log.d(TAG, "averaging between " + framesToAverage.size() + " images");
            outs.add(new MatrixInfo(getAverage(framesToAverage), t.index));
        }

        Collections.sort(outs, new MatrixIndexComparator());
        for (int i = 0; i < outs.size(); i++){
 //           imwrite("C:\\Users\\utsav\\Pictures\\images\\img" + i + ".jpg", outs.get(i).m);
        }

        // for (int i = 0; i < MAX_COUNT; i++) {
        //     MatrixInfo t = mats.get((int)(Math.random() * MAX_COUNT));
        //     imwrite("C:\\Users\\utsav\\Pictures\\images\\img" + i + ".jpg", t.m);
        // }
        Log.d(TAG, "Done saving images");
        return -1;
    }

    // manually gets the averages of a list of matrices
    // we implemented this by hand because the inbuilt accumulate only works with floats
    // and we wanted to shrink the size of our data
    private static Mat getAverage(List<Mat> mats) {
        if (mats.size() == 0) {
            throw new RuntimeException("no mats");
        }
        Mat avgImg = mats.get(0).clone();
        MatExpr expr = new MatExpr(avgImg);
        for (int i = 1; i < mats.size(); i++) {
            Mat m = mats.get(i);
            expr = opencv_core.add(m, expr);
        }
        return opencv_core.multiply(expr.asMat(), 1.0/mats.size()).asMat();
    }

    @Override
    protected void onPostExecute(Integer result) {
        Log.d(TAG, "In postexecute");
        progressDialog.dismiss();
        Activity activity = (Activity) mContext;
        BBProcActivity bbProcActivity = (BBProcActivity) activity;
        if (bbProcActivity == null) {
            return;
        }
        if (result == -1) {
            TextView textView = (TextView) bbProcActivity.findViewById(R.id.text_result);
            textView.setText("Error, could not process the video correctly!");
            bbProcActivity.setCurrResult(result);
            return;
        }
        BBProcWorker worker = new BBProcWorker(bbProcActivity);
        worker.execute(folder);
    }
}
