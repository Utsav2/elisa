package uiuc.bioassay.elisa;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import uiuc.bioassay.elisa.R;

public class CalibInfoActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calib_info);
        TextView redLaserText = (TextView) findViewById(R.id.red_laser_text);
        redLaserText.setText("Red laser: \t\t\t" + ELISAApplication.RED_LASER_PEAK + " (in pixels)");

        TextView greenLaserText = (TextView) findViewById(R.id.green_laser_text);
        greenLaserText.setText("Green laser: \t" + ELISAApplication.GREEN_LASER_PEAK + " (in pixels)");

        double nmScale = (ELISAApplication.RED_LASER_NM - ELISAApplication.GREEN_LASER_NM) /
                (ELISAApplication.RED_LASER_PEAK - ELISAApplication.GREEN_LASER_PEAK);
        TextView nmScaleText = (TextView) findViewById(R.id.nm_scale_text);
        nmScaleText.setText("nm per pixel: \t" + Math.abs(nmScale));

        Button newAbsExpButton = (Button) findViewById(R.id.new_abs_exp);
        newAbsExpButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CalibInfoActivity.this, ExpIntroActivity.class);
                        intent.putExtra(ELISAApplication.MODE_EXTRA, ELISAApplication.MODE_ABSORPTION);
                        startActivity(intent);
                        finish();
                    }
                }
        );

        Button newFluorescentExpButton = (Button) findViewById(R.id.new_flourescent_exp);
        newFluorescentExpButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(CalibInfoActivity.this, ExpIntroActivity.class);
                        intent.putExtra(ELISAApplication.MODE_EXTRA, ELISAApplication.MODE_FLUORESCENT);
                        startActivity(intent);
                        finish();
                    }
                }
        );

        Button newElisaExpButton = (Button) findViewById(R.id.new_elisa_exp);
        newElisaExpButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CalibInfoActivity.this, ExpIntroActivity.class);
                        intent.putExtra(ELISAApplication.MODE_EXTRA, ELISAApplication.MODE_ELISA);
                        startActivity(intent);
                        finish();
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_calib_info, menu);
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
