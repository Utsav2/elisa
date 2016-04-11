package uiuc.bioassay.elisa;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class ELISAGraphActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elisagraph);
        Intent intent = getIntent();
        double[] finalResult = intent.getDoubleArrayExtra(ELISAApplication.ELISA_ABS_RESULT);
        double[] stds = intent.getDoubleArrayExtra(ELISAApplication.ELISA_STDS);
        LineChart chart = (LineChart) findViewById(R.id.elisa_result_chart);
        setData(stds, finalResult, chart);
        Button doneGraph = (Button) findViewById(R.id.doneGraph);
        doneGraph.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                }
        );
    }

    private void setData(double[] x, double[] y, LineChart lineChart) {
        ArrayList<String> xVals = new ArrayList<>();
        for (int i = 0; i < x.length; ++i) {
            xVals.add((float) x[i] + "");
        }
        ArrayList<Entry> yVals = new ArrayList<>();
        for (int i = 0; i < y.length; ++i) {
            Log.d("xxx", "" + y[i]);
            yVals.add(new Entry((float) y[i], i));
        }
        LineDataSet lineDataSet = new LineDataSet(yVals, "Measured data");
        lineDataSet.setDrawCircles(true);
        lineDataSet.setLineWidth(1f);

        ArrayList<LineDataSet> lineDataSets = new ArrayList<>();
        lineDataSets.add(lineDataSet); // add the datasets
        LineData data = new LineData(xVals, lineDataSets);
        lineChart.setData(data);
        lineChart.invalidate();
        lineChart.setDescription("Concentration");
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_elisagraph, menu);
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
}
