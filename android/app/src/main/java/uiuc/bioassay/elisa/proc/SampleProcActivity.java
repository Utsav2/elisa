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

import java.io.File;
import java.util.ArrayList;

import uiuc.bioassay.elisa.ELISAApplication;
import uiuc.bioassay.elisa.R;

import static uiuc.bioassay.elisa.ELISAApplication.AVG_FILE_NAME;
import static uiuc.bioassay.elisa.ELISAApplication.VIDEO_EXTRA;
import static uiuc.bioassay.elisa.ELISAApplication.readBBResNormalized;
import static uiuc.bioassay.elisa.ELISAApplication.readRGBSpec;
import static uiuc.bioassay.elisa.ELISAApplication.readSampleResNormalized;

public class SampleProcActivity extends AppCompatActivity {
    private static final String TAG = "SAMPLE";
    private int currResult;
    private File folder;
    private Bitmap bitmap;
    private double[] rgb_spec;
    private double[] bb;
    private double[] sample;
    private double[] absData;
    private Button currButton = null;

    public void setCurrResult(int result) {
        currResult = result;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_proc);

        folder = new File(getIntent().getStringExtra(ELISAApplication.FOLDER_EXTRA));
        final Intent intent = getIntent();

        Button done = (Button) findViewById(R.id.done);
        done.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (intent.getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                            if (sample == null) {
                                // sample = readSampleResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample" + File.separator + ELISAApplication.RES);
                                // TODO: Enable the below in production
                                sample = readSampleResNormalized(folder.getAbsolutePath() + File.separator + ELISAApplication.RES);
                            }
                            if (bb == null) {
                                // bb = readBBResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/bb" + File.separator + ELISAApplication.RES);
                                // TODO: Enable the below in production
                                if (intent.getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                                    bb = readBBResNormalized(folder.getParentFile().getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
                                } else if (intent.getAction().equals(ELISAApplication.ACTION_ONE_SAMPLE)) {
                                    bb = readBBResNormalized(folder.getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
                                }
                            }
                            double resAbsData = 0;
                            if (absData == null) {
                                absData = new double[bb.length];
                                for (int i = 0; i < bb.length; ++i) {
                                    absData[i] = bb[i] - sample[i];
                                }
                            }
                            int procMode = intent.getIntExtra(ELISAApplication.ELISA_PROC_MODE, 0);
                            if (procMode == ELISAApplication.ELISA_PROC_MODE_FULL_INTEGRATION) {
                                for (int i = 0; i < absData.length; ++i) {
                                    resAbsData += absData[i];
                                }
                            } else {
                                double nmScale = (ELISAApplication.RED_LASER_NM - ELISAApplication.GREEN_LASER_NM) /
                                        (ELISAApplication.RED_LASER_PEAK - ELISAApplication.GREEN_LASER_PEAK);

                                double nmOff = ELISAApplication.GREEN_LASER_NM - nmScale * ELISAApplication.GREEN_LASER_PEAK;

                                double[] nm = new double[absData.length];
                                for (int i = 0; i < absData.length; ++i) {
                                    nm[i] = nmScale * i + nmOff;
                                }
                                int idx = 0;
                                while (idx < absData.length && nm[idx] >= 450) {
                                    ++idx;
                                }
                                assert(idx >= 1 && (idx < absData.length - 1));
                                double a = (nm[idx - 1] - 450);
                                double b = (450 - nm[idx]);
                                Log.d(TAG, "" + a + ", " + b);
                                resAbsData = (b/(a + b)) * absData[idx - 1] + (a / (a + b)) * absData[idx];
                            }
                            ELISAApplication.currentSampleIdx = intent.getIntExtra(ELISAApplication.INT_EXTRA, -1);
                            ELISAApplication.resultSampleAbs = resAbsData;
                        }
                        finish();
                    }
                }
        );

        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        final LineChart chart = (LineChart) findViewById(R.id.chart);

        final Button imageButton = (Button) findViewById(R.id.image_button);
        currButton = imageButton;
        imageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currResult == -1) {
                            return;
                        }
                        currButton.setEnabled(true);
                        chart.setVisibility(View.INVISIBLE);
                        if (bitmap == null) {
                            // bitmap = decodeIMG("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample" + File.separator + AVG_FILE_NAME);
                            // TODO: Enable the below in production
                            bitmap = decodeIMG(folder.getAbsolutePath() + File.separator + "1.jpg");
                        }
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                        imageButton.setEnabled(false);
                        currButton = imageButton;
                    }
                }
        );

        final Button rgbButton = (Button) findViewById(R.id.rgb_button);
        rgbButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currResult == -1) {
                            return;
                        }
                        currButton.setEnabled(true);
                        imageView.setVisibility(View.INVISIBLE);
                        if (rgb_spec == null) {
                            // rgb_spec = readRGBSpec("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample" + File.separator + ELISAApplication.RGB_SPEC);
                            // TODO: Enable the below in production
                            rgb_spec = readRGBSpec(folder.getAbsolutePath() + File.separator + ELISAApplication.RGB_SPEC);
                        }
                        setRGBSpecData(chart);
                        chart.setVisibility(View.VISIBLE);
                        rgbButton.setEnabled(false);
                        currButton = rgbButton;
                    }
                }
        );

        final Button normalizedButton = (Button) findViewById(R.id.normalized_button);
        normalizedButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currResult == -1) {
                            return;
                        }
                        currButton.setEnabled(true);
                        imageView.setVisibility(View.INVISIBLE);
                        if (sample == null) {
                            // sample = readSampleResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample" + File.separator + ELISAApplication.RES);
                            // TODO: Enable the below in production
                            sample = readSampleResNormalized(folder.getAbsolutePath() + File.separator + ELISAApplication.RES);
                        }
                        if (bb == null) {
                            // bb = readBBResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/bb" + File.separator + ELISAApplication.RES);
                            // TODO: Enable the below in production
                             /* Get grandparent */
                            if (intent.getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                                bb = readBBResNormalized(folder.getParentFile().getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
                            } else if (intent.getAction().equals(ELISAApplication.ACTION_ONE_SAMPLE)) {
                                bb = readBBResNormalized(folder.getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
                            }
                        }
                        setSampleAndBBData(chart);
                        chart.setVisibility(View.VISIBLE);
                        normalizedButton.setEnabled(false);
                        currButton = normalizedButton;
                    }
                }
        );

        final Button absButton = (Button) findViewById(R.id.abs_button);
        absButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currResult == -1) {
                            return;
                        }
                        currButton.setEnabled(true);
                        imageView.setVisibility(View.INVISIBLE);
                        if (sample == null) {
                            // sample = readSampleResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample" + File.separator + ELISAApplication.RES);
                            // TODO: Enable the below in production
                            sample = readSampleResNormalized(folder.getAbsolutePath() + File.separator + ELISAApplication.RES);
                        }
                        if (bb == null) {
                            // bb = readBBResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/bb" + File.separator + ELISAApplication.RES);
                            // TODO: Enable the below in production
                            if (intent.getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                                /* Get grandparent */
                                bb = readBBResNormalized(folder.getParentFile().getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
                            } else if (intent.getAction().equals(ELISAApplication.ACTION_ONE_SAMPLE)) {
                                bb = readBBResNormalized(folder.getParent() + File.separator + ELISAApplication.BB_FOLDER + File.separator + ELISAApplication.RES);
                            }
                        }
                        if (absData == null) {
                            absData = new double[bb.length];
                            for (int i = 0; i < bb.length; ++i) {
                                absData[i] = bb[i] - sample[i];
                            }
                        }
                        setAbsData(chart);
                        chart.setVisibility(View.VISIBLE);
                        absButton.setEnabled(false);
                        currButton = absButton;
                    }
                }
        );

        SampleProcWorker sampleProcWorker = new SampleProcWorker(this);

        // sampleProcWorker.execute("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/sample");
        // TODO: Enable the below in production
        if (intent.getAction().equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
            sampleProcWorker.execute(folder.getAbsolutePath(), ELISAApplication.ACTION_MULTIPLE_SAMPLE);
        } else if (intent.getAction().equals(ELISAApplication.ACTION_ONE_SAMPLE)){
            sampleProcWorker.execute(folder.getAbsolutePath(), ELISAApplication.ACTION_ONE_SAMPLE);
        } else if(intent.getAction().equals(ELISAApplication.ACTION_VIDEO_SAMPLE)) {
            new FluoroscentWorker(this).execute(folder.getAbsolutePath(),
                    getIntent().getExtras().getString(VIDEO_EXTRA));
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

    private void setRGBSpecData(LineChart lineChart) {
        int length = rgb_spec.length / 3;

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
            red[i-startIdx] = rgb_spec[3*i];
            green[i-startIdx] = rgb_spec[3*i + 1];
            blue[i-startIdx] = rgb_spec[3*i + 2];
        }


        ArrayList<Entry> yRedVals = new ArrayList<>();
        ArrayList<Entry> yGreenVals = new ArrayList<>();
        ArrayList<Entry> yBlueVals = new ArrayList<>();

        for (int i = endIdx; i >= startIdx; --i) {
            int idx = endIdx - i;
            yRedVals.add(new Entry((float)red[i-startIdx], idx));
            yGreenVals.add(new Entry((float)green[i-startIdx], idx));
            yBlueVals.add(new Entry((float)blue[i-startIdx], idx));
        }


        LineDataSet setRed = new LineDataSet(yRedVals, "Red Spectrum");
        setRed.setDrawCircles(false);
        setRed.setColor(Color.RED);
        setRed.setLineWidth(1f);

        LineDataSet setGreen = new LineDataSet(yGreenVals, "Green Spectrum");
        setGreen.setDrawCircles(false);
        setGreen.setColor(Color.GREEN);
        setGreen.setLineWidth(1f);

        LineDataSet setBlue = new LineDataSet(yBlueVals, "Blue Spectrum");
        setBlue.setDrawCircles(false);
        setBlue.setColor(Color.BLUE);
        setBlue.setLineWidth(1f);

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setRed); // add the datasets
        dataSets.add(setGreen);
        dataSets.add(setBlue);


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
        int length = sample.length;

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
            ySampleVals.add(new Entry((float)sample[i], idx));
            yBBVals.add(new Entry((float)bb[i], idx));
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
        int length = sample.length;

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
            yVals.add(new Entry((float)absData[i], idx));
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
