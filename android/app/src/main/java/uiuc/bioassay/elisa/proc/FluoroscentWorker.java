package uiuc.bioassay.elisa.proc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import uiuc.bioassay.elisa.R;

import java.io.File;

import static uiuc.bioassay.elisa.ELISAApplication.processVideo;

/**
 * Created by utsav on 11/11/2016.
 */

public class FluoroscentWorker extends AsyncTask<String, Void, Integer> {

    private Context mContext;
    private ProgressDialog progressDialog;
    private String folder;

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
        return processVideo(videoFile);
    }

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
            textView.setText("Error, could not process the video correctly!");
            bbProcActivity.setCurrResult(result);
            return;
        }
        BBProcWorker worker = new BBProcWorker(bbProcActivity);
        worker.execute(folder);
    }
}
