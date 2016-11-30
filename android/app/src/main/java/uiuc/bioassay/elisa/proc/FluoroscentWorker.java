package uiuc.bioassay.elisa.proc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.*;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import uiuc.bioassay.elisa.ELISAApplication;
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
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.accumulate;
import static uiuc.bioassay.elisa.ELISAApplication.processBB;
import static uiuc.bioassay.elisa.ELISAApplication.processF;

public class FluoroscentWorker extends AsyncTask<String, Void, Integer> {

    private static final String TAG = "FluoroscentWorker";

    protected Context mContext;
    protected ProgressDialog progressDialog;
    protected String folder;

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
        startProcessingVideo(assetToVideoFile("MOV_0051.MP4"));
        int ret = processF(folder + File.separator);
        Log.d(TAG, "here " + ret);
        return ret;
    }

    // a helper method only present to test the code with a pre-existing mp4 asset
    private String assetToVideoFile(String name) {
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
        MatrixInfo(Mat m, int index, double mean) {
            this.m = m;
            this.index = index;
            this.mean = mean;
        }

        // when we don't need the mean
        MatrixInfo(Mat m, int index) {
            this(m, index, -1);
        }
    }

    // used to sort matrices based on when we saw them
    private static class MatrixIndexComparator implements Comparator<MatrixInfo> {
        @Override
        public int compare(MatrixInfo matrixInfo, MatrixInfo t1) {
            return matrixInfo.index - t1.index;
        }
    }

    // helps to get the frames with highest mean values
    private static class MatrixMeanComparator implements Comparator<MatrixInfo> {
        @Override
        public int compare(MatrixInfo o1, MatrixInfo o2) {
            return (int) (o2.mean - o1.mean);
        }
    }

    private void startProcessingVideo(String videoFile) {
        FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(videoFile);
        videoGrabber.setFormat("mp4");
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        List<MatrixInfo> mats = new ArrayList<>();
        PriorityQueue<MatrixInfo> infoPriorityQueue = new PriorityQueue<>(
                ELISAApplication.MAX_PICTURE, // initial capacity
                new MatrixMeanComparator()
        );

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
                // this is assetToVideoFile heavy operation, that for some reason even on deallocation
                // takes up memory (enough to force Android to kill the app -- after killing
                // other processes). I'm not sure if it's simply creation of the mat objects,
                // it's probably the conversion
                Mat mat = new Mat();
                m.convertTo(mat, CV_16S);
                // end heavy operation

                main._deallocate();
                mat._deallocate();
                m._deallocate();
                MatrixInfo matrixInfo = new MatrixInfo(mat, count, mean);
                Log.v(TAG, "count: " + count + "mean: " + matrixInfo.mean);
                mats.add(matrixInfo);
                if (mean > max) {
                    max = mean;
                }
                infoPriorityQueue.add(matrixInfo);
                count++;
            } catch (FrameGrabber.Exception e) {
                Log.e(TAG, "exception in retreiving assetToVideoFile frame", e);
                break;
            }
        };

        double threshold = 0.2 * max;
        Log.v(TAG, "threshold: " + threshold);

        List<MatrixInfo> outs = new ArrayList<>();

        for (int i = 0; i < ELISAApplication.MAX_PICTURE; i++) {
            List<Mat> framesToAverage = new ArrayList<>();
            MatrixInfo t = infoPriorityQueue.remove();
            framesToAverage.add(t.m);
            // now remove from the sides of this peak until we reach threshold
            int leftIndex = t.index - 1;
            int rightIndex = t.index + 1;
            for(int j = leftIndex; j >= 0; j--) {
                MatrixInfo m = mats.get(j);
                if (m.mean < threshold) {
                    break;
                }
                infoPriorityQueue.remove(m);
                framesToAverage.add(m.m);
            }

            for (int j = rightIndex; j < count; j++) {
                MatrixInfo m = mats.get(j);
                if (m.mean < threshold) {
                    break;
                }
                infoPriorityQueue.remove(m);
                framesToAverage.add(m.m);
            }
            Log.v(TAG, "averaging between " + framesToAverage.size() + " images");
            outs.add(new MatrixInfo(getAverage(framesToAverage), t.index));
        }

        Collections.sort(outs, new MatrixIndexComparator());
        for (int i = 0; i < outs.size(); i++){
            String path = folder + File.separator + (i+1) + "/";
            new File(path).mkdirs();
            String f = path + "image.jpg";
            Log.v(TAG, "Saving image at: " + f);
            imwrite(f, outs.get(i).m);
        }
        Log.d(TAG, "Done saving images");
    }

    // manually gets the averages of assetToVideoFile list of matrices
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

    int current = 1;

    @Override
    protected void onPostExecute(Integer result) {
        progressDialog.dismiss();
        Activity activity = (Activity) mContext;
        BBProcActivity bbProcActivity = (BBProcActivity) activity;
        if (bbProcActivity == null) {
            return;
        }
        if (result == -1) {
            TextView textView = (TextView) bbProcActivity.findViewById(R.id.text_result);
            textView.setText("Error, unable to process data!");
            bbProcActivity.setCurrResult(result);
            return;
        }

        final ImageView imageView = (ImageView) bbProcActivity.findViewById(R.id.imageView);
        imageView.setImageBitmap(decodeIMG(folder + "/1/image.jpg"));

        Button previous = (Button) bbProcActivity.findViewById(R.id.previous);
        previous.setVisibility(View.VISIBLE);

        Button next = (Button) bbProcActivity.findViewById(R.id.next);
        next.setVisibility(View.VISIBLE);

        Button button = (Button) bbProcActivity.findViewById(R.id.image_button);
        button.setEnabled(false);
        bbProcActivity.setResult(result);
    }

    private static Bitmap decodeIMG(String img) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeFile(img, options);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
