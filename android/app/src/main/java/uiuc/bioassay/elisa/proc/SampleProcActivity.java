package uiuc.bioassay.elisa.proc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.bytedeco.javacpp.opencv_core;

import java.io.File;
import java.util.ArrayList;

import uiuc.bioassay.elisa.ELISAApplication;
import uiuc.bioassay.elisa.R;

import static org.bytedeco.javacpp.opencv_core.max;
import static org.bytedeco.javacpp.opencv_core.mean;
import static uiuc.bioassay.elisa.ELISAApplication.FLUOROSCENT_FRAME_AREA_THRESHOLD;
import static uiuc.bioassay.elisa.ELISAApplication.FLUOROSCENT_LASER_AND_DATA_AREA_THRESHOLD;
import static uiuc.bioassay.elisa.ELISAApplication.MODE_FLUORESCENT;
import static uiuc.bioassay.elisa.ELISAApplication.VIDEO_EXTRA;
import static uiuc.bioassay.elisa.ELISAApplication.readBBResNormalized;
import static uiuc.bioassay.elisa.ELISAApplication.readRGBSpec;
import static uiuc.bioassay.elisa.ELISAApplication.readSampleResNormalized;

public class SampleProcActivity extends AppCompatActivity {

    class NmScaleInfo {
        int startIdx;
        int endIdx;
        double [] nm;

        NmScaleInfo(int startIdx, int endIdx, double [] nm) {
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.nm = nm;
        }
    }

    private static final String TAG = "SAMPLE";
    private int currResult;
    private File folder;
    private Button imageButton;
    private Button normalizedButton;
    private Button absButton;
    private ImageView view;
    private String mode;
    private Button rgbButton;
    private Bitmap [] bitmaps;
    private double[][] rgb_spec;
    private double[][] bb;
    private double[][] sample;
    private double[][] absData;
    private double[][] preprocessedFluoroscentData;
    private double[][] normalizedFluoroscentData;
    private double[] laserAreas;
    private double laserAreasAverage;
    private double laserAreasMax = -1;
    private double[] dataAreas;
    private Button currButton;
    private LineChart chart;
    private int numPeaks;

    private void setSample(Intent intent) {
        if (intent.getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
            setSampleForCurrent();
            setBBForCurrent();
            setAbsForCurrent();
            double resAbsData = 0;
            int procMode = intent.getIntExtra(ELISAApplication.ELISA_PROC_MODE, 0);
            if (mode.equals(ELISAApplication.MODE_ELISA)) {
                if (procMode == ELISAApplication.ELISA_PROC_MODE_FULL_INTEGRATION) {
                    for (int i = 0; i < absData[current].length; ++i) {
                        resAbsData += absData[current][i];
                    }
                } else {
                    double nmScale = (ELISAApplication.RED_LASER_NM - ELISAApplication.GREEN_LASER_NM) /
                            (ELISAApplication.RED_LASER_PEAK - ELISAApplication.GREEN_LASER_PEAK);

                    double nmOff = ELISAApplication.GREEN_LASER_NM - nmScale * ELISAApplication.GREEN_LASER_PEAK;

                    double[] nm = new double[absData[current].length];
                    for (int i = 0; i < absData[current].length; ++i) {
                        nm[i] = nmScale * i + nmOff;
                    }
                    int idx = 0;
                    while (idx < absData[current].length && nm[idx] >= 450) {
                        ++idx;
                    }
                    assert (idx >= 1 && (idx < absData[current].length - 1));
                    double a = (nm[idx - 1] - 450);
                    double b = (450 - nm[idx]);
                    resAbsData = (b / (a + b)) * absData[current][idx - 1] + (a / (a + b)) * absData[current][idx];
                }
                ELISAApplication.currentSampleIdx = intent.getIntExtra(ELISAApplication.INT_EXTRA, -1);
                ELISAApplication.resultSampleAbs = resAbsData;
            } else if (mode.equals(ELISAApplication.MODE_FLUORESCENT)) {
                reassignCurrent();
                setRgbForCurrent();
                setNormalizedFluoroscentData();
                ELISAApplication.currentSampleIdx = intent.getIntExtra(ELISAApplication.INT_EXTRA, -2);
                ELISAApplication.currentNormalizedAreas = new double[numPeaks];
                for (int i = 0; i < numPeaks; i++) {
                    ELISAApplication.currentNormalizedAreas[i] = Math.sqrt(dataAreas[i]) /
                            Math.sqrt(Math.sqrt(laserAreas[i] * laserAreasAverage));
                }
            }
        }
        finish();
    }

    private void setActualImageView(ImageView imageView) {
        reassignCurrent();
        if (currResult == -1) {
            return;
        }
        currButton.setEnabled(true);
        chart.setVisibility(View.INVISIBLE);
        if (bitmaps[current] == null) {
            // bitmaps = decodeIMG("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample" + File.separator + AVG_FILE_NAME);
            // TODO: Enable the below in production
            Log.d(TAG, "folder: " + folder.toString());
            if (mode.equals(ELISAApplication.MODE_FLUORESCENT) && getIntent().getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
               bitmaps[current] = decodeIMG(folder.getAbsolutePath() + File.separator + (current+1) + "/image.jpg");
            } else {
                bitmaps[current] = decodeIMG(folder.getAbsolutePath() + File.separator + "1.jpg");
            }
        }
        imageView.setImageBitmap(bitmaps[current]);
        imageView.setVisibility(View.VISIBLE);
        imageButton.setEnabled(false);
        currButton = imageButton;
    }

    private int current = 0;
    private void reassignCurrent() {
        if (current < 0) {
            current = 0;
        }
        if (current >= ELISAApplication.MAX_PICTURE) {
            current = ELISAApplication.MAX_PICTURE - 1;
        }
    }

    private void setRgbView() {
        reassignCurrent();
        if (currResult == -1) {
            return;
        }
        currButton.setEnabled(true);
        view.setVisibility(View.INVISIBLE);
        setRgbForCurrent();
        setRGBSpecData(chart);
        chart.setVisibility(View.VISIBLE);
        rgbButton.setEnabled(false);
        currButton = rgbButton;
    }

    private void setRgbForIndex(int i) {
        if (rgb_spec[i] == null) {
            // rgb_spec = readRGBSpec("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample" + File.separator + ELISAApplication.RGB_SPEC);
            // TODO: Enable the below in production
            if (mode.equals(ELISAApplication.MODE_FLUORESCENT) && getIntent().getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                rgb_spec[i] = readRGBSpec(folder.getAbsolutePath() + File.separator + (i+1) + File.separator + ELISAApplication.RGB_SPEC);
            } else {
                rgb_spec[i] = readRGBSpec(folder.getAbsolutePath() + File.separator + ELISAApplication.RGB_SPEC);
            }
        }
    }

    private void setRgbForCurrent() {
        setRgbForIndex(current);
    }

    private void setRgbForAll() {
        for (int i = 0; i < numPeaks; i++) {
            setRgbForIndex(i);
        }
    }

    void setSampleForCurrent() {
        if (sample[current] == null) {
            // sample = readSampleResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample" + File.separator + ELISAApplication.RES);
            // TODO: Enable the below in production
            if (mode.equals(ELISAApplication.MODE_FLUORESCENT) && getIntent().getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                sample[current] = readSampleResNormalized(folder.getAbsolutePath() + File.separator + (current+1) + File.separator + ELISAApplication.RES);
            } else {
                sample[current] = readSampleResNormalized(folder.getAbsolutePath() + File.separator + ELISAApplication.RES);
            }
        }
    }

    private void setNormalizedView() {
        reassignCurrent();
        if (currResult == -1) {
            return;
        }
        currButton.setEnabled(true);
        view.setVisibility(View.INVISIBLE);
        if (mode.equals(MODE_FLUORESCENT)) {
            setNormalizedFluoroscentData();
            setNormalizedFluoroscentChart(chart);
        } else {
            setBBForCurrent();
            setSampleForCurrent();
            setSampleAndBBData(chart);
        }
        chart.setVisibility(View.VISIBLE);
        normalizedButton.setEnabled(false);
        currButton = normalizedButton;
    }

    private void setBBForCurrent() {
        if (bb[current] == null) {
            // bb = readBBResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/bb" + File.separator + ELISAApplication.RES);
            // TODO: Enable the below in production
            if (mode.equals(ELISAApplication.MODE_FLUORESCENT) && getIntent().getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                bb[current] = readBBResNormalized(folder.getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
            } else if (getIntent().getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                bb[current] = readBBResNormalized(folder.getParentFile().getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
            } else if (getIntent().getAction().equals(ELISAApplication.ACTION_ONE_SAMPLE)) {
                bb[current] = readBBResNormalized(folder.getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
            }
        }
    }

    private void setAbsForCurrent() {
        if (absData[current] == null) {
            absData[current] = new double[bb[current].length];
            setSampleForCurrent();
            for (int i = 0; i < bb.length; ++i) {
                absData[current][i] = bb[current][i] - sample[current][i];
            }
        }
    }

    private void setAbsView(Intent intent) {
        reassignCurrent();
        if (currResult == -1) {
            return;
        }
        setSampleForCurrent();
        setBBForCurrent();
        setAbsForCurrent();
        currButton.setEnabled(true);
        view.setVisibility(View.INVISIBLE);
        setAbsData(chart);
        chart.setVisibility(View.VISIBLE);
        absButton.setEnabled(false);
        currButton = absButton;
    }

    public void setCurrResult(int result) {
        currResult = result;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_proc);

        folder = new File(getIntent().getStringExtra(ELISAApplication.FOLDER_EXTRA));

        final Intent intent = getIntent();
        mode = intent.getStringExtra(ELISAApplication.MODE_EXTRA);

        numPeaks = 8;
        // Todo(utsav) enable in prod
        // if there's no peaks set, then we're not in fluoroscent mode, where everything has only
        // one val
        // numPeaks = getIntent().getIntExtra(ELISAApplication.NUM_PEAKS, 1);

        bitmaps = new Bitmap[numPeaks];
        rgb_spec = new double[numPeaks][];
        bb = new double[numPeaks][];
        sample = new double[numPeaks][];
        absData = new double[numPeaks][];

        dataAreas = new double[numPeaks];
        laserAreas = new double[numPeaks];

        for (int i = 0; i < numPeaks; i++) {
            dataAreas[i] = 0.0;
            laserAreas[i] = 0.0;
        }

        laserAreas = new double[numPeaks];
        preprocessedFluoroscentData = new double[numPeaks][];
        normalizedFluoroscentData = new double[numPeaks][];

        Button done = (Button) findViewById(R.id.done);
        done.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setSample(intent);
                    }
                }
        );

        view = (ImageView) findViewById(R.id.imageView);
        chart = (LineChart) findViewById(R.id.chart);

        imageButton = (Button) findViewById(R.id.image_button);
        currButton = imageButton;
        imageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setActualImageView(view);
                    }
                }
        );

        rgbButton = (Button) findViewById(R.id.rgb_button);
        rgbButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setRgbView();
                    }
                }
        );

        normalizedButton = (Button) findViewById(R.id.normalized_button);
        normalizedButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setNormalizedView();
                    }
                }
        );

        absButton = (Button) findViewById(R.id.abs_button);

        if (mode.equals(ELISAApplication.MODE_FLUORESCENT)) {
            // disable AbsView
            absButton.setVisibility(View.GONE);
        }
        absButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setAbsView(intent);
                    }
                }
        );

        Button prevButton = (Button)findViewById(R.id.previous);
        Button nextButton = (Button)findViewById(R.id.next);

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                current--;
                if (currButton.equals(imageButton)) {
                    setActualImageView(view);
                } else if (currButton.equals(rgbButton)) {
                    setRgbView();
                } else if (currButton.equals(normalizedButton)) {
                    setNormalizedView();
                } else if (currButton.equals(absButton)){
                    setAbsView(intent);
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                current++;
                if (currButton.equals(imageButton)) {
                    setActualImageView(view);
                } else if (currButton.equals(rgbButton)) {
                    setRgbView();
                } else if (currButton.equals(normalizedButton)) {
                    setNormalizedView();
                } else if (currButton.equals(absButton)){
                    setAbsView(intent);
                }
            }
        });

        // sampleProcWorker.execute("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample");
        // TODO: Enable the below in production
        String mode = getIntent().getStringExtra(ELISAApplication.MODE_EXTRA);
        if(intent.getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE) && mode.equals(ELISAApplication.MODE_FLUORESCENT)) {
            new FluoroscentWorker(this).execute(folder.getAbsolutePath(),
                    getIntent().getExtras().getString(VIDEO_EXTRA), String.valueOf(getIntent().getExtras().getInt(ELISAApplication.NUM_PEAKS, -1)));
        } else if (intent.getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
            SampleProcWorker sampleProcWorker = new SampleProcWorker(this);
            sampleProcWorker.execute(folder.getAbsolutePath(), ELISAApplication.ACTION_MULTIPLE_SAMPLE);
        } else if (intent.getAction().equals(ELISAApplication.ACTION_ONE_SAMPLE)){
            SampleProcWorker sampleProcWorker = new SampleProcWorker(this);
            sampleProcWorker.execute(folder.getAbsolutePath(), ELISAApplication.ACTION_ONE_SAMPLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_elisaproc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
    }

    private static Bitmap decodeIMG(String img) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeFile(img, options);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void setAreasAll() {
        for (int i = 0; i < numPeaks; i++) {
            setArea(i);
        }
        opencv_core.Mat m = new opencv_core.Mat(laserAreas);
        laserAreasAverage = mean(m).magnitude();
        m._deallocate();
    }

    public static double cumtrapz(double [] vec, int min, int max) {

        double sum = 0;
        double cumulative = 0;

        for(int i = min; i < max; i++){
            sum += (vec[i]+vec[i+1])/2.0;
            cumulative += sum;
        }
        return cumulative;
    }

    private void setArea(int index) {

        // it's already been set
        if (Math.abs(dataAreas[index]) > 2 * Double.MIN_VALUE) {
            return;
        }

        double [] input = preprocessedFluoroscentData[index];

        double max = -1;
        int maxIndex = -1;

        // first find the max
        for (int i = 0; i < input.length; i ++) {
            if (input[i] > max) {
                max = input[i];
                maxIndex = i;
            }
        }

        double thresholdValue = max * FLUOROSCENT_LASER_AND_DATA_AREA_THRESHOLD;

        int stopLeftIndex = maxIndex - 1;
        int stopRightIndex = maxIndex + 1;

        while (stopLeftIndex >= 0 && input[stopLeftIndex] > thresholdValue) {
            stopLeftIndex--;
        }

        while(stopRightIndex < input.length && input[stopRightIndex] > thresholdValue) {
            stopRightIndex++;
        }

        double firstIntegral = cumtrapz(input, stopLeftIndex, stopRightIndex);

        // now find the second peak not between this data
        double max2 = -1;
        int max2Index = -1;

        for (int i = 0; i < stopLeftIndex; i++) {
            if (max2 < input[i]) {
                max2 = input[i];
                max2Index = i;
            }
        }

        for (int i = stopRightIndex; i < input.length; i++) {
            if (max2 < input[i]) {
                max2 = input[i];
                max2Index = i;
            }
        }

        int stopLeftIndex2 = max2Index - 1;
        int stopRightIndex2 = max2Index + 1;

        boolean isLeft = max2Index < maxIndex;

        if (isLeft) {
            while (stopLeftIndex2 >= 0 && input[stopLeftIndex2] > thresholdValue) {
                stopLeftIndex2--;
            }

            while (stopRightIndex2 < stopLeftIndex && input[stopRightIndex2] > thresholdValue) {
                stopRightIndex2++;
            }
        } else {
            while (stopLeftIndex2 >= stopRightIndex  && input[stopLeftIndex2] > thresholdValue) {
                stopLeftIndex2--;
            }

            while (stopRightIndex2 < input.length && input[stopRightIndex2] > thresholdValue) {
                stopRightIndex2++;
            }
        }

        // now calculate integration of the second one
        double secondIntegral = cumtrapz(input, stopLeftIndex2, stopRightIndex2);

        // check which width is smaller, Laser area should always be the one with the lesser area.
        boolean laserFirst = (stopRightIndex - stopLeftIndex) < (stopRightIndex2 - stopLeftIndex2);

        double laserArea;
        double dataArea;

        if (laserFirst) {
            laserArea = firstIntegral;
            dataArea = secondIntegral;
        } else {
            laserArea = secondIntegral;
            dataArea = firstIntegral;
        }

        laserAreas[index] = laserArea;
        dataAreas[index] = dataArea;
    }

    private NmScaleInfo calcNm(int length) {

        double nmScale = (ELISAApplication.RED_LASER_NM - ELISAApplication.GREEN_LASER_NM) /
                (ELISAApplication.RED_LASER_PEAK - ELISAApplication.GREEN_LASER_PEAK);

        double nmOff = ELISAApplication.GREEN_LASER_NM - nmScale * ELISAApplication.GREEN_LASER_PEAK;

        double [] nm = new double[length];
        for (int i = 0; i < length; ++i) {
            nm[i] = nmScale * i + nmOff;
        }

        int startIdx = 0;
        while (nm[startIdx] > 700) {
            ++startIdx;
        }

        int endIdx = startIdx;
        while (endIdx < (length - 1) && nm[endIdx] >= 380) {
            ++endIdx;
        }

        return new NmScaleInfo(startIdx, endIdx, nm);
    }

    private void setPreprocessedFluoroscentDataAll() {

        // its already been set for all
        if (preprocessedFluoroscentData[0] != null) {
            return;
        }

        setRgbForAll();

        NmScaleInfo nmScaleInfo = calcNm(rgb_spec[0].length / 3);
        int startIdx = nmScaleInfo.startIdx, endIdx = nmScaleInfo.endIdx;

        for (int index = 0; index < numPeaks; index++) {
            preprocessedFluoroscentData[index] = new double[endIdx - startIdx + 1];
            for (int i = startIdx; i <= endIdx; ++i) {
                preprocessedFluoroscentData[index][i - startIdx] =
                        (rgb_spec[index][3 * i] + rgb_spec[index][3 * i + 1] + rgb_spec[index][3 * i + 2]);
            }
        }
    }

    private void setNormalizedFluoroscentData() {

        setPreprocessedFluoroscentDataAll();

        for (int index = 0; index < numPeaks; index++) {

            if (normalizedFluoroscentData[index] == null) {

                setRgbForAll();
                setAreasAll();

                NmScaleInfo nmScaleInfo = calcNm(rgb_spec[index].length / 3);
                int startIdx  = nmScaleInfo.startIdx, endIdx = nmScaleInfo.endIdx;

                normalizedFluoroscentData[index] = new double[endIdx - startIdx + 1];
                for (int i = startIdx; i <= endIdx; ++i) {
                    double dataPoint = preprocessedFluoroscentData[index][i - startIdx] /
                                    Math.sqrt(Math.sqrt(laserAreas[index] * laserAreasAverage));

                    if (dataPoint > laserAreasMax) {
                        laserAreasMax = dataPoint;
                    }
                    normalizedFluoroscentData[index][i - startIdx] = dataPoint;
                }
            }
        }
    }

    private void setRGBSpecData(LineChart lineChart) {
        lineChart.invalidate();
        lineChart.getAxisLeft().resetAxisMaxValue();

        reassignCurrent();
        setPreprocessedFluoroscentDataAll();

        ArrayList<Entry> yRedVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();

        NmScaleInfo nmScaleInfo = calcNm(rgb_spec[current].length/3);

        int startIdx = nmScaleInfo.startIdx, endIdx = nmScaleInfo.endIdx;
        double [] nm = nmScaleInfo.nm;

        for (int i = endIdx; i >= startIdx; --i) {
            xVals.add((float) nm[i] + "");
        }

        for (int i = endIdx; i >= startIdx; --i) {
            int idx = endIdx - i;
            yRedVals.add(new Entry((float)(preprocessedFluoroscentData[current][i - startIdx]), idx));
        }

        LineDataSet setRed = new LineDataSet(yRedVals, "Color Spectrum");
        setRed.setDrawCircles(false);
        setRed.setColor(Color.WHITE);
        setRed.setLineWidth(1f);

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setRed); // add the datasets


        LineData data = new LineData(xVals, dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
        lineChart.setDescription("Wavelength [nm]");
        lineChart.setDescriptionColor(Color.WHITE);

        // XAxis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);


        // YAxis
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);

        // Legend
        Legend legend = lineChart.getLegend();
        legend.setTextSize(10f);
        legend.setTextColor(Color.WHITE);

        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setScaleXEnabled(false);
        lineChart.setScaleYEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setHighlightEnabled(false);
        lineChart.setHighlightPerDragEnabled(false);
        lineChart.setDrawGridBackground(false);
    }

    private void setNormalizedFluoroscentChart(LineChart lineChart) {
        reassignCurrent();
        setNormalizedFluoroscentData();

        lineChart.invalidate();
        lineChart.getAxisLeft().setAxisMaxValue((float)laserAreasMax);

        NmScaleInfo nmScaleInfo = calcNm(rgb_spec[current].length/3);

        int startIdx = nmScaleInfo.startIdx, endIdx = nmScaleInfo.endIdx;
        double [] nm = nmScaleInfo.nm;

        ArrayList<String> xVals = new ArrayList<>();
        for (int i = endIdx; i >= startIdx; --i) {
            xVals.add((float) nm[i] + "");
        }

        ArrayList<Entry> yRedVals = new ArrayList<>();

        for (int i = endIdx; i >= startIdx; --i) {
            int idx = endIdx - i;
            yRedVals.add(new Entry((float)(normalizedFluoroscentData[current][i - startIdx]), idx));
        }


        LineDataSet setRed = new LineDataSet(yRedVals, "Normalized Color Spectrum");
        setRed.setDrawCircles(false);
        setRed.setColor(Color.WHITE);
        setRed.setLineWidth(1f);

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setRed); // add the datasets


        LineData data = new LineData(xVals, dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
        lineChart.setDescription("Wavelength [nm]");
        lineChart.setDescriptionColor(Color.WHITE);

        // XAxis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);


        // YAxis
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);

        // Legend
        Legend legend = lineChart.getLegend();
        legend.setTextSize(10f);
        legend.setTextColor(Color.WHITE);

        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setScaleXEnabled(false);
        lineChart.setScaleYEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setHighlightEnabled(false);
        lineChart.setHighlightPerDragEnabled(false);
        lineChart.setDrawGridBackground(false);
    }

    private void setSampleAndBBData(LineChart lineChart) {
        lineChart.invalidate();
        lineChart.getAxisLeft().resetAxisMaxValue();

        int length = sample[current].length;

        double nmScale = (ELISAApplication.RED_LASER_NM - ELISAApplication.GREEN_LASER_NM) /
                (ELISAApplication.RED_LASER_PEAK - ELISAApplication.GREEN_LASER_PEAK);

        double nmOff = ELISAApplication.GREEN_LASER_NM - nmScale * ELISAApplication.GREEN_LASER_PEAK;

        double[] nm = new double[length];
        for (int i = 0; i < length; ++i) {
            nm[i] = nmScale * i + nmOff;
        }

        int startIdx = 0;
        while (nm[startIdx] > 700) {
            ++startIdx;
        }
        int endIdx = startIdx;
        while (endIdx < (length - 1) && nm[endIdx] >= 380) {
            ++endIdx;
        }

        ArrayList<String> xVals = new ArrayList<>();
        for (int i = endIdx; i >= startIdx; --i) {
            xVals.add((float) nm[i] + "");
        }

        ArrayList<Entry> ySampleVals = new ArrayList<>();
        ArrayList<Entry> yBBVals = new ArrayList<>();

        for (int i = endIdx; i >= startIdx; --i) {
            int idx = endIdx - i;
            ySampleVals.add(new Entry((float)sample[current][i], idx));
            yBBVals.add(new Entry((float)bb[current][i], idx));
        }


        LineDataSet sampleSet = new LineDataSet(ySampleVals, "Normalized Sample Spectrum");
        sampleSet.setDrawCircles(false);
        sampleSet.setColor(Color.MAGENTA);
        sampleSet.setLineWidth(1f);

        LineDataSet bbSet = new LineDataSet(yBBVals, "Normalized Broadband Spectrum");
        bbSet.setDrawCircles(false);
        bbSet.setColor(Color.CYAN);
        bbSet.setLineWidth(1f);

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(sampleSet); // add the datasets
        dataSets.add(bbSet);

        LineData data = new LineData(xVals, dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
        lineChart.setDescription("Wavelength [nm]");
        lineChart.setDescriptionColor(Color.WHITE);

        // XAxis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);


        // YAxis
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.resetAxisMaxValue();
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);

        // Legend
        Legend legend = lineChart.getLegend();
        legend.setTextSize(10f);
        legend.setTextColor(Color.WHITE);

        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setScaleXEnabled(false);
        lineChart.setScaleYEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setHighlightEnabled(false);
        lineChart.setHighlightPerDragEnabled(false);
        lineChart.setDrawGridBackground(false);
    }

    private void setAbsData(LineChart lineChart) {
        lineChart.invalidate();
        lineChart.getAxisLeft().resetAxisMaxValue();

        lineChart.invalidate();
        lineChart.getAxisLeft().resetAxisMaxValue();

        int length = sample[current].length;

        double nmScale = (ELISAApplication.RED_LASER_NM - ELISAApplication.GREEN_LASER_NM) /
                (ELISAApplication.RED_LASER_PEAK - ELISAApplication.GREEN_LASER_PEAK);

        double nmOff = ELISAApplication.GREEN_LASER_NM - nmScale * ELISAApplication.GREEN_LASER_PEAK;

        double[] nm = new double[length];
        for (int i = 0; i < length; ++i) {
            nm[i] = nmScale * i + nmOff;
        }

        int startIdx = 0;
        while (nm[startIdx] > 700) {
            ++startIdx;
        }

        int endIdx = startIdx;
        while (endIdx < (length - 1) && nm[endIdx] >= 380) {
            ++endIdx;
        }

        ArrayList<String> xVals = new ArrayList<>();
        for (int i = endIdx; i >= startIdx; --i) {
            xVals.add((float) nm[i] + "");
        }

        ArrayList<Entry> yVals = new ArrayList<>();

        for (int i = endIdx; i >= startIdx; --i) {
            int idx = endIdx - i;
            yVals.add(new Entry((float)absData[current][i], idx));
        }


        LineDataSet absSet = new LineDataSet(yVals, "Normalized Absorption Spectrum");
        absSet.setDrawCircles(false);
        absSet.setColor(Color.YELLOW);
        absSet.setLineWidth(1f);

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(absSet); // add the datasets


        LineData data = new LineData(xVals, dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
        lineChart.setDescription("Wavelength [nm]");
        lineChart.setDescriptionColor(Color.WHITE);

        // XAxis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);


        // YAxis
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.resetAxisMaxValue();
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);

        // Legend
        Legend legend = lineChart.getLegend();
        legend.setTextSize(10f);
        legend.setTextColor(Color.WHITE);

        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setScaleXEnabled(false);
        lineChart.setScaleYEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setHighlightEnabled(false);
        lineChart.setHighlightPerDragEnabled(false);
        lineChart.setDrawGridBackground(false);
    }
}
