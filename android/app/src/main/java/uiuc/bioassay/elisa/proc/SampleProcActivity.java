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

import static org.bytedeco.javacpp.opencv_core.mean;
import static uiuc.bioassay.elisa.ELISAApplication.MODE_FLUORESCENT;
import static uiuc.bioassay.elisa.ELISAApplication.VIDEO_EXTRA;
import static uiuc.bioassay.elisa.ELISAApplication.readBBResNormalized;
import static uiuc.bioassay.elisa.ELISAApplication.readRGBSpec;
import static uiuc.bioassay.elisa.ELISAApplication.readSampleResNormalized;

public class SampleProcActivity extends AppCompatActivity {
    private static final String TAG = "SAMPLE";
    private int currResult;
    private File folder;
    private Button imageButton;
    private Button normalizedButton;
    private Button absButton;
    private ImageView view;
    private String mode;
    Button rgbButton;
    private Bitmap [] bitmaps;
    private double[][] rgb_spec;
    private double[][] bb;
    private double[][] sample;
    private double[][] absData;
    private double[][] normalizedFluoroscentData;
    private double[] laserAreas;
    private double laserAreasAverage;
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
                    Log.d(TAG, "" + a + ", " + b);
                    resAbsData = (b / (a + b)) * absData[current][idx - 1] + (a / (a + b)) * absData[current][idx];
                }
                ELISAApplication.currentSampleIdx = intent.getIntExtra(ELISAApplication.INT_EXTRA, -1);
                ELISAApplication.resultSampleAbs = resAbsData;
            } else if (mode.equals(ELISAApplication.MODE_FLUORESCENT)) {
                reassignCurrent();
                setRgbForCurrent();
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
                rgb_spec[i] = readRGBSpec(folder.getAbsolutePath() + File.separator + (current+1) + File.separator + ELISAApplication.RGB_SPEC);
            } else {
                rgb_spec[i] = readRGBSpec(folder.getAbsolutePath() + File.separator + ELISAApplication.RGB_SPEC);
            }
        }
    }

    private void setRgbForCurrent() {
        setRgbForIndex(current);
    }

    private void setRgbForAll() {
        for (int i = 0; i < getIntent().getIntExtra(ELISAApplication.NUM_PEAKS, -1); i++) {
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
        laserAreasAverage = mean(new opencv_core.Mat(laserAreas)).magnitude();
    }

    private void setArea(int index) {
        if (Math.abs(dataAreas[index]) > 2 * Double.MIN_VALUE) {
            return;
        }
    }

    private void setNormalizedFluoroscentData(int index) {

        if (normalizedFluoroscentData[index] == null) {

            setRgbForIndex(index);
            setAreasAll();

            int length = rgb_spec[index].length / 3;

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

            double[] colorVals = new double[endIdx - startIdx + 1];
            for (int i = startIdx; i <= endIdx; ++i) {
                colorVals[i - startIdx] = (rgb_spec[current][3 * i] + rgb_spec[current][3 * i + 1] + +rgb_spec[current][3 * i + 2]);
                colorVals[i - startIdx] /= Math.sqrt(Math.sqrt(laserAreas[current] * laserAreasAverage));
            }

            normalizedFluoroscentData[current] = colorVals;
        }
    }

    private void setNormalizedFluoroscentData() {
        reassignCurrent();
        setNormalizedFluoroscentData(current);
    }

    private void setNormalizedFluoroscentDataAll() {
        for (int i = 0; i < numPeaks; i++) {
            setNormalizedFluoroscentData(i);
        }
    }

    private void setRGBSpecData(LineChart lineChart) {
        int length = rgb_spec[current].length / 3;

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

        double[] red = new double[endIdx - startIdx + 1];
        double[] green = new double[endIdx - startIdx + 1];
        double[] blue = new double[endIdx - startIdx + 1];
        for (int i = startIdx; i <= endIdx; ++i) {
            red[i-startIdx] = rgb_spec[current][3*i];
            green[i-startIdx] = rgb_spec[current][3*i + 1];
            blue[i-startIdx] = rgb_spec[current][3*i + 2];
        }


        ArrayList<Entry> yRedVals = new ArrayList<>();
//        ArrayList<Entry> yGreenVals = new ArrayList<>();
//        ArrayList<Entry> yBlueVals = new ArrayList<>();

        for (int i = endIdx; i >= startIdx; --i) {
            int idx = endIdx - i;
            yRedVals.add(new Entry((float)(red[i-startIdx] + green[i - startIdx] + blue[i - startIdx]), idx));
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
        int length = rgb_spec[current].length / 3;

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


        ArrayList<Entry> yRedVals = new ArrayList<>();

        for (int i = endIdx; i >= startIdx; --i) {
            int idx = endIdx - i;
            yRedVals.add(new Entry((float)(normalizedFluoroscentData[current][idx]), idx));
        }


        LineDataSet setRed = new LineDataSet(yRedVals, "Normalized Color Spectrum");
        setRed.setDrawCircles(false);
        setRed.setColor(Color.WHITE);
        setRed.setLineWidth(1f);

//        LineDataSet setGreen = new LineDataSet(yGreenVals, "Green Spectrum");
//        setGreen.setDrawCircles(false);
//        setGreen.setColor(Color.GREEN);
//        setGreen.setLineWidth(1f);

//        LineDataSet setBlue = new LineDataSet(yBlueVals, "Blue Spectrum");
//        setBlue.setDrawCircles(false);
//        setBlue.setColor(Color.BLUE);
//        setBlue.setLineWidth(1f);

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setRed); // add the datasets
//        dataSets.add(setGreen);
//        dataSets.add(setBlue);


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
