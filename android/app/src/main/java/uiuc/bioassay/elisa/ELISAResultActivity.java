package uiuc.bioassay.elisa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;
import uiuc.bioassay.elisa.camera.CameraActivity;

import static uiuc.bioassay.elisa.ELISAApplication.ELISA_PROC_MODE;
import static uiuc.bioassay.elisa.ELISAApplication.MODE_FLUORESCENT;
import static uiuc.bioassay.elisa.ELISAApplication.round;

public class ELISAResultActivity extends AppCompatActivity {
    private static final String TAG = "ELISA Result";
    private TableLayout tableLayout;
    private static final int COLUMN_WIDTH = 155;
    private String folder;
    private int numStds;
    private String mode;
    private int maxNumReplicates;
    private double[] absorptions;
    private double[][] fluoroscentResults;
    private int[] numReplicates;
    private EditText[] editTexts;
    private boolean dataRetrieving = false;
    private int procMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elisaresult);
        Intent intent = getIntent();
        numStds = intent.getIntExtra(ELISAApplication.NUM_STDS, 0);
        maxNumReplicates = intent.getIntExtra(ELISAApplication.MAX_NUM_REPLICATES, 0);
        mode = intent.getStringExtra(ELISAApplication.MODE_EXTRA);
        folder = intent.getStringExtra(ELISAApplication.FOLDER_EXTRA);
        procMode = intent.getIntExtra(ELISA_PROC_MODE, 0);
        if (mode.equals(MODE_FLUORESCENT)) {
            drawFluoroscentTable(maxNumReplicates);
        }
        drawTable(numStds, maxNumReplicates);
        absorptions = new double[numStds];
        fluoroscentResults = new double[maxNumReplicates][];

        for (int i = 0; i < absorptions.length; ++i) {
            absorptions[i] = 0;
        }
        numReplicates = new int[numStds];
        for (int i = 0; i < numReplicates.length; ++i) {
            numReplicates[i] = 0;
        }
        Button resultNext = (Button) findViewById(R.id.elisa_result_next);
        resultNext.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        if (!mode.equals(MODE_FLUORESCENT)) {
                                            double[] finalResult = new double[absorptions.length];
                                            for (int i = 0; i < finalResult.length; ++i) {
                                                Log.d(TAG, "" + numReplicates[i]);
                                                if (numReplicates[i] == 0) {
                                                    finalResult[i] = 0;
                                                } else {
                                                    finalResult[i] = absorptions[i] / numReplicates[i];
                                                }
                                            }
                                            double[] stds = new double[absorptions.length];
                                            for (int i = 0; i < stds.length; ++i) {
                                                if (editTexts[i].getText().toString().equals("")) {
                                                    Toast.makeText(ELISAResultActivity.this, "Please fill in the std concentration", Toast.LENGTH_LONG).show();
                                                    editTexts[i].requestFocus();
                                                    return;
                                                }
                                                stds[i] = Double.parseDouble(editTexts[i].getText().toString());
                                            }
                                            Intent intentGraph = new Intent(ELISAResultActivity.this, ELISAGraphActivity.class);
                                            intentGraph.putExtra(ELISAApplication.ELISA_ABS_RESULT, finalResult);
                                            intentGraph.putExtra(ELISAApplication.ELISA_STDS, stds);
                                            startActivity(intentGraph);
                                            finish();
                                        } else {
                                            double[] finalResult = new double[numStds];
                                            for (int i = 0; i < finalResult.length; ++i) {
                                                double sum = 0;
                                                for (int j = 0; j < maxNumReplicates; j++) {
                                                    if (fluoroscentResults[j] == null) {
                                                        Toast.makeText(ELISAResultActivity.this, "Please take all the replicates", Toast.LENGTH_LONG).show();
                                                        return;
                                                    }
                                                    sum += fluoroscentResults[j][i];
                                                }
                                                finalResult[i] = sum / maxNumReplicates;
                                            }
                                            double[] stds = new double[numStds];
                                            for (int i = 0; i < stds.length; ++i) {
                                                stds[i] = Double.parseDouble(editTexts[i].getText().toString());
                                            }
                                            Intent intentGraph = new Intent(ELISAResultActivity.this, ELISAGraphActivity.class);
                                            intentGraph.putExtra(ELISAApplication.ELISA_ABS_RESULT, finalResult);
                                            intentGraph.putExtra(ELISAApplication.ELISA_STDS, stds);
                                            startActivity(intentGraph);
                                            if (getIntent().getBooleanExtra(ELISAApplication.SAVE_TO_DISK, false)) {
                                                saveToDisk(stds, finalResult);
                                            }
                                            finish();
                                        }
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                            }
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(ELISAResultActivity.this);
                        builder.setTitle("Are you sure to go next?")
                                .setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("No", dialogClickListener).show();
                    }
                }
        );
        ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view);
        scrollView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // Check if the view with focus is EditText
                Log.d(TAG, getCurrentFocus() + "");
                if (getCurrentFocus() instanceof EditText) {
                    Log.d(TAG, "Got you");
                    EditText ed = (EditText) getCurrentFocus();
                    if (ed.hasFocus()) {

                        // Hide the keyboard
                        InputMethodManager inputManager = (InputMethodManager)
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                                InputMethodManager.HIDE_NOT_ALWAYS);
                        // Clear the focus
                        ed.clearFocus();
                    }
                }
                return false;
            }
        });
    }

    private void saveToDisk(double [] stds, double [] finalResults) {

        if (BuildConfig.DEBUG && (stds.length != finalResults.length)) {
            throw new RuntimeException("lengths should be the same: " + stds.length + " " + finalResults.length);
        }

        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File newFile = new File(downloads, System.currentTimeMillis() + ".csv");
        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(newFile.getAbsolutePath()), ',');
            for (int i = 0; i < stds.length; i++) {
                String [] line = { String.valueOf(stds[i]), String.valueOf(finalResults[i]) };
                csvWriter.writeNext(line);
            }
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "error", e);
        }
        Toast.makeText(ELISAResultActivity.this, "Saving to disk: " + newFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_elisaresult, menu);
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

    void drawFluoroscentTable(final int numSamples) {
        findViewById(R.id.f_table_text).setVisibility(View.VISIBLE);
        TextView tv = (TextView)findViewById(R.id.result_table_text);
        tv.setText("Fluoroscent result table");
        ScrollView fScrollView = (ScrollView)findViewById(R.id.scroll_view_f);
        fScrollView.setVisibility(View.VISIBLE);
        TableLayout fTableLayout = (TableLayout)findViewById(R.id.fluoroscent_table);
        TableLayout.LayoutParams lp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT);
        TableRow headerRow = new TableRow(this);

        TextView placeHolder = new TextView(this);
        placeHolder.setMinimumWidth(COLUMN_WIDTH);
        placeHolder.setVisibility(View.INVISIBLE);
        headerRow.addView(placeHolder);

        for (int i = 1; i < numSamples + 1; ++i) {
            TextView textView = new TextView(this);
            textView.setMinimumWidth(COLUMN_WIDTH);
            textView.setGravity(Gravity.CENTER);
            textView.setText("" + i);
            headerRow.addView(textView);
        }

        fTableLayout.addView(headerRow);

        TableRow tableRow = new TableRow(this);
        TextView anotherPlaceholder = new TextView(this);
        anotherPlaceholder.setMinimumWidth(COLUMN_WIDTH);
        anotherPlaceholder.setVisibility(View.INVISIBLE);
        tableRow.addView(anotherPlaceholder);

        for (int i = 1; i < numSamples + 1; ++i) {
            final Button button = new Button(this);
            button.setMinimumWidth(COLUMN_WIDTH);
            button.setGravity(Gravity.CENTER);
            final int finalI = i;
            button.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for (EditText editText : editTexts)
                            if (editText.getText().toString().equals("")) {
                                Toast.makeText(ELISAResultActivity.this, "Please fill in the std concentration", Toast.LENGTH_LONG).show();
                                editText.requestFocus();
                                return;
                            }
                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            dataRetrieving = true;
                                            Intent intent = new Intent(ELISAResultActivity.this, CameraActivity.class);
                                            intent.putExtra(ELISAApplication.MODE_EXTRA, getIntent().getStringExtra(ELISAApplication.MODE_EXTRA));
                                            intent.setAction(ELISAApplication.ACTION_MULTIPLE_SAMPLE);
                                            intent.putExtra(ELISAApplication.NUM_PEAKS, numStds);
                                            intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder + File.separator);
                                            intent.putExtra(ELISAApplication.ELISA_PROC_MODE, procMode);
                                            intent.putExtra(ELISAApplication.INT_EXTRA, finalI);
                                            startActivity(intent);
                                            break;

                                        case DialogInterface.BUTTON_NEGATIVE:
                                            //No button clicked
                                            break;
                                    }
                                }
                            };
                            AlertDialog.Builder builder = new AlertDialog.Builder(ELISAResultActivity.this);
                            builder.setTitle("Are you sure that you want to take a new sample?")
                                    .setPositiveButton("Yes", dialogClickListener)
                                    .setNegativeButton("No", dialogClickListener).show();
                        }
                    }
            );
            tableRow.addView(button);
        }
        fTableLayout.addView(tableRow);
    }

    void drawTable(final int nRows, final int nCols) {

        tableLayout = (TableLayout) findViewById(R.id.result_table);
        TableLayout.LayoutParams lp = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
        );

        // Initialize header column
        TableRow headerRow = new TableRow(this);
        headerRow.setLayoutParams(lp);
        TextView placeHolder = new TextView(this);
        placeHolder.setMinimumWidth(COLUMN_WIDTH);
        placeHolder.setVisibility(View.INVISIBLE);
        headerRow.addView(placeHolder);
        for (int i = 1; i < nCols + 1; ++i) {
            TextView textView = new TextView(this);
            textView.setMinimumWidth(COLUMN_WIDTH);
            textView.setGravity(Gravity.CENTER);
            textView.setText("" + i);
            headerRow.addView(textView);
        }
        TextView titleAvg = new TextView(this);
        titleAvg.setMinimumWidth(COLUMN_WIDTH);
        titleAvg.setGravity(Gravity.CENTER);
        titleAvg.setText("AVG");
        headerRow.addView(titleAvg);
        tableLayout.addView(headerRow);

        editTexts = new EditText[nRows];
        for (int i = 0; i < nRows; ++i) {
            TableRow tableRow = new TableRow(this);
            final EditText editText = new EditText(this);
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(7);
            editText.setFilters(filterArray);
            editText.setMinimumWidth(COLUMN_WIDTH);
            editText.setInputType(2);
            editText.setOnFocusChangeListener(
                    new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (!hasFocus) {
                                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            }
                        }
                    }
            );
            editTexts[i] = editText;
            tableRow.addView(editText);
            if (!mode.equals(ELISAApplication.MODE_FLUORESCENT)) {
                for (int j = 0; j < nCols; ++j) {
                    final Button button = new Button(this);
                    button.setMinimumWidth(COLUMN_WIDTH);
                    button.setGravity(Gravity.CENTER);
                    final int finalI = i;
                    final int finalJ = j;
                    button.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (editText.getText().toString().equals("")) {
                                        Toast.makeText(ELISAResultActivity.this, "Please fill in the std concentration", Toast.LENGTH_LONG).show();
                                        editText.requestFocus();
                                        return;
                                    }
                                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    dataRetrieving = true;
                                                    Intent intent = new Intent(ELISAResultActivity.this, CameraActivity.class);
                                                    intent.putExtra(ELISAApplication.MODE_EXTRA, getIntent().getStringExtra(ELISAApplication.MODE_EXTRA));
                                                    intent.setAction(ELISAApplication.ACTION_MULTIPLE_SAMPLE);
                                                    intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder + File.separator + editText.getText().toString() + File.separator + (finalJ + 1));
                                                    Log.d(TAG, "int Extra: " + (finalI * nCols + finalJ));
                                                    intent.putExtra(ELISAApplication.INT_EXTRA, finalI * nCols + finalJ);
                                                    intent.putExtra(ELISAApplication.ELISA_PROC_MODE, procMode);
                                                    startActivity(intent);
                                                    break;

                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    //No button clicked
                                                    break;
                                            }
                                        }
                                    };
                                    AlertDialog.Builder builder = new AlertDialog.Builder(ELISAResultActivity.this);
                                    builder.setTitle("Are you sure that you want to take new sample?").setMessage("Sample Information: " + "\n  - Std Value: " + getText(editText) +
                                            "\n  - Current Value : " + getText(button))
                                            .setPositiveButton("Yes", dialogClickListener)
                                            .setNegativeButton("No", dialogClickListener).show();
                                }
                            }
                    );
                    tableRow.addView(button);
                }
            } else {
                for (int j = 0; j < nCols; ++j) {
                    TextView dataPoint = new TextView(this);
                    dataPoint.setMinimumWidth(COLUMN_WIDTH);
                    dataPoint.setText("0.00");
                    dataPoint.setGravity(Gravity.CENTER);
                    tableRow.addView(dataPoint);
                }
            }

            TextView avg = new TextView(this);
            avg.setMinimumWidth(COLUMN_WIDTH);
            avg.setText("0.00");
            avg.setGravity(Gravity.CENTER);
            tableRow.addView(avg);
            tableLayout.addView(tableRow);
        }
    }

    private String getText(EditText editText) {
        String s = editText.getText().toString();
        return (s.equals("") ? "N/A" : s);
    }

    private String getText(Button button) {
        String s = button.getText().toString();
        return (s.equals("") ? "N/A" : s);
    }

    protected void onResume() {
        super.onResume();
        if (dataRetrieving) {
            if (!mode.equals(MODE_FLUORESCENT)) {
                Log.d(TAG, "zzzzz: " + ELISAApplication.currentSampleIdx);
                int i = ELISAApplication.currentSampleIdx / maxNumReplicates;
                int j = ELISAApplication.currentSampleIdx % maxNumReplicates;
                TableRow tableRow = (TableRow) tableLayout.getChildAt(i + 1);
                Button button = (Button) tableRow.getChildAt(j + 1);
                TextView textView = (TextView) tableRow.getChildAt(maxNumReplicates + 1);
                String oldButtonText = button.getText().toString();
                Log.d(TAG, ELISAApplication.resultSampleAbs + "");
                if (oldButtonText.equals("")) {
                    absorptions[i] += ELISAApplication.resultSampleAbs;
                    numReplicates[i] += 1;
                } else {
                    double oldResult = Double.parseDouble(oldButtonText);
                    absorptions[i] = absorptions[i] - oldResult + ELISAApplication.resultSampleAbs;
                }
                button.setText("" + round(ELISAApplication.resultSampleAbs, 2));
                textView.setText("" + round(absorptions[i] / numReplicates[i], 2));
                dataRetrieving = false;
            } else {
                int i = ELISAApplication.currentSampleIdx - 1;
                Log.d(TAG, "currentSampleIdx" + i);
                fluoroscentResults[i] =  ELISAApplication.currentNormalizedAreas.clone();
                for (int j = 0; j < numStds; j++) {
                    TableRow tableRow = (TableRow) tableLayout.getChildAt(j + 1);
                    double sum = 0;
                    TextView tv = (TextView) tableRow.getChildAt(i + 1);
                    tv.setText("" + round(fluoroscentResults[i][j], 2));
                    int setK = 0;
                    for (int k = 0; k < maxNumReplicates; k++) {
                        if (fluoroscentResults[k] != null) {
                            setK++;
                            sum += fluoroscentResults[k][j];
                        }
                    }
                    TextView averageTextView = (TextView) tableRow.getChildAt(maxNumReplicates + 1);
                    if (setK != 0) {
                        averageTextView.setText("" + round(sum / setK, 2));
                    }
                }
            }
            dataRetrieving = false;
        }
    }
}
