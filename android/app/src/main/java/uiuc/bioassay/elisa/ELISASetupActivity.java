package uiuc.bioassay.elisa;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;


public class ELISASetupActivity extends ActionBarActivity {
    private int procMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elisasetup);
        final CheckBox ctv = (CheckBox)findViewById(R.id.save_to_disk_box);
        if (ELISAApplication.MODE_FLUORESCENT.equals(getIntent().getStringExtra(ELISAApplication.MODE_EXTRA))) {
            TextView tv = (TextView)findViewById(R.id.setup_elisa);
            tv.setText("Fluoroscent setup");
            TextView another = (TextView)findViewById(R.id.elisa_setup);
            another.setText("Fluoroscent setup");
            findViewById(R.id.type_of_processing_text).setVisibility(View.INVISIBLE);
            findViewById(R.id.type_of_processing_group).setVisibility(View.INVISIBLE);
            findViewById(R.id.save_to_disk).setVisibility(View.VISIBLE);
            findViewById(R.id.type_of_processing_group).setVisibility(View.INVISIBLE);
            ctv.setVisibility(View.VISIBLE);
        }

        final Spinner numStdsSpinner = (Spinner) findViewById(R.id.num_stds);
        final Spinner numReplicatesSpinner = (Spinner) findViewById(R.id.num_replicates);
        Button nextSetup = (Button) findViewById(R.id.next_setup);
        nextSetup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ELISASetupActivity.this, ELISAResultActivity.class);
                        intent.putExtra(ELISAApplication.MODE_EXTRA, getIntent().getStringExtra(ELISAApplication.MODE_EXTRA));
                        intent.putExtra(ELISAApplication.FOLDER_EXTRA, getIntent().getStringExtra(ELISAApplication.FOLDER_EXTRA));
                        intent.putExtra(ELISAApplication.NUM_STDS, Integer.parseInt(numStdsSpinner.getSelectedItem().toString()));
                        intent.putExtra(ELISAApplication.MAX_NUM_REPLICATES, Integer.parseInt(numReplicatesSpinner.getSelectedItem().toString()));
                        intent.putExtra(ELISAApplication.ELISA_PROC_MODE, procMode);
                        intent.putExtra(ELISAApplication.SAVE_TO_DISK, ctv.isChecked());
                        startActivity(intent);
                        finish();
                    }
                }
        );

    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_450nm:
                if (checked)
                    procMode = ELISAApplication.ELISA_PROC_MODE_450nm;
                break;
            case R.id.radio_full_integration:
                if (checked)
                    procMode = ELISAApplication.ELISA_PROC_MODE_FULL_INTEGRATION;
                break;
        }
    }

    @Override
    public void onBackPressed() {

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_elisasetup, menu);
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
