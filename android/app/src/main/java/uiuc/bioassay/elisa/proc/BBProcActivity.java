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
import uiuc.bioassay.elisa.ELISASetupActivity;
import uiuc.bioassay.elisa.R;
import uiuc.bioassay.elisa.camera.CameraActivity;

import static uiuc.bioassay.elisa.ELISAApplication.AVG_FILE_NAME;
import static uiuc.bioassay.elisa.ELISAApplication.readBBResNormalized;
import static uiuc.bioassay.elisa.ELISAApplication.readRGBSpec;

public class BBProcActivity extends AppCompatActivity {
    private static final String TAG = "BB";
    private int currResult;
    private String mode;
    private File folder;
    private Bitmap bitmap;
    private double[] rgb_spec;
    private double[] bb;
    private Button currButton = null;

    public void setCurrResult(int result) {
        currResult = result;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bb_proc);

        mode = getIntent().getStringExtra(ELISAApplication.MODE_EXTRA);
        folder = new File(getIntent().getStringExtra(ELISAApplication.FOLDER_EXTRA));

        Button done = (Button) findViewById(R.id.done);
        done.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currResult == -1) {
                            Intent intent = new Intent(BBProcActivity.this, CameraActivity.class);
                            intent.setAction(ELISAApplication.ACTION_BROADBAND);
                            intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder.getAbsolutePath());
                            startActivity(intent);
                        } else if (mode.equals(ELISAApplication.MODE_ABSORPTION)){
                            Intent intent = new Intent(BBProcActivity.this, CameraActivity.class);
                            intent.setAction(ELISAApplication.ACTION_ONE_SAMPLE);
                            intent.putExtra(ELISAApplication.MODE_EXTRA, mode);
                            intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder.getParent() + File.separator + ELISAApplication.SAMPLE_FOLDER);
                            startActivity(intent);
                        } else if (mode.equals(ELISAApplication.MODE_FLUORESCENT)) {
                            Intent intent = new Intent(BBProcActivity.this, ELISASetupActivity.class);
                            intent.setAction(ELISAApplication.ACTION_MULTIPLE_SAMPLE);
                            intent.putExtra(ELISAApplication.MODE_EXTRA, mode);
                            intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder.getParent() + File.separator + ELISAApplication.SAMPLE_FOLDER);
                            startActivity(intent);
                        } else if (mode.equals(ELISAApplication.MODE_ELISA)) {
                            Intent intent = new Intent(BBProcActivity.this, ELISASetupActivity.class);
                            intent.putExtra(ELISAApplication.MODE_EXTRA, mode);
                            intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder.getParent());
                            startActivity(intent);
                        }
                        finish();
                    }
                }
        );

        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        final LineChart chart = (LineChart) findViewById(R.id.chart);

        final Button image_button = (Button) findViewById(R.id.image_button);
        currButton = image_button;
        image_button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currResult == -1) {
                            return;
                        }
                        currButton.setEnabled(true);
                        chart.setVisibility(View.INVISIBLE);
                        if (bitmap == null) {
                            // bitmap = decodeIMG("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/bb" + File.separator + AVG_FILE_NAME);
                            // TODO: Enable the below in production
                            bitmap = decodeIMG(folder + File.separator + "1.jpg");
                        }
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                        image_button.setEnabled(false);
                        currButton = image_button;
                    }
                }
        );

        final Button rgb_button = (Button) findViewById(R.id.rgb_button);
        rgb_button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currResult == -1) {
                            return;
                        }
                        currButton.setEnabled(true);
                        imageView.setVisibility(View.INVISIBLE);
                        if (rgb_spec == null) {
                            // rgb_spec = readRGBSpec("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/bb" + File.separator + ELISAApplication.RGB_SPEC);
                            // TODO: Enable the below in production
                            rgb_spec = readRGBSpec(folder + File.separator + ELISAApplication.RGB_SPEC);
                        }
                        setRGBSpecData(chart);
                        chart.setVisibility(View.VISIBLE);
                        rgb_button.setEnabled(false);
                        currButton = rgb_button;
                    }
                }
        );

        final Button normalized_button = (Button) findViewById(R.id.normalized_button);
        normalized_button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currResult == -1) {
                            return;
                        }
                        currButton.setEnabled(true);
                        imageView.setVisibility(View.INVISIBLE);
                        if (bb == null) {
                            // bb = readBBResNormalized("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/bb" + File.separator + ELISAApplication.RES);
                            // TODO: Enable the below in production
                            bb = readBBResNormalized(folder + File.separator + ELISAApplication.RES);
                        }
                        setBBData(chart);
                        chart.setVisibility(View.VISIBLE);
                        normalized_button.setEnabled(false);
                        currButton = normalized_button;
                    }
                }
        );

        BBProcWorker bbProcWorker = new BBProcWorker(this);

        // bbProcWorker.execute("/storage/emulated/0/Android/data/uiuc.bioassay.elisa/test-elisa/bb");
        // TODO: Enable the below in production
        bbProcWorker.execute(folder.getAbsolutePath());
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

    private void setBBData(LineChart lineChart) {
        int length = bb.length;

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
            yVals.add(new Entry((float)bb[i], idx));
        }


        LineDataSet lineDataSet = new LineDataSet(yVals, "Normalized Broadband Spectrum");
        lineDataSet.setDrawCircles(false);
        lineDataSet.setColor(Color.CYAN);
        lineDataSet.setLineWidth(1f);

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet); // add the datasets


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
