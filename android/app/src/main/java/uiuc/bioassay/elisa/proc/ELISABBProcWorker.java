package uiuc.bioassay.elisa.proc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;

import uiuc.bioassay.elisa.ELISAApplication;
import uiuc.bioassay.elisa.R;
import uiuc.bioassay.elisa.services.NetworkService;

import static uiuc.bioassay.elisa.ELISAApplication.AVG_FILE_NAME;
import static uiuc.bioassay.elisa.ELISAApplication.LOG_FILE;
import static uiuc.bioassay.elisa.ELISAApplication.SAMPLE_FOLDER;
import static uiuc.bioassay.elisa.ELISAApplication.processBBELISA;

/**
 * Created by meowle on 7/8/15.
 */
public class ELISABBProcWorker extends AsyncTask<String, Void, double[]> {
    private Context mContext;
    private ProgressDialog progressDialog;
    private String folder;

    public ELISABBProcWorker(Context context) {
        mContext = context;
        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Wait");
        progressDialog.setMessage("Processing BroadBand...");
        progressDialog.show();
    }

    @Override
    protected double[] doInBackground(String... params) {
        folder = params[0];
        return processBBELISA(folder + File.separator);
    }

    @Override
    protected void onPostExecute(final double[] spots) {
        progressDialog.dismiss();
        for (int i = 0; i < ELISAApplication.MAX_PICTURE; ++i) {
            NetworkService.startActionUpload(mContext, folder + File.separator + SAMPLE_FOLDER + AVG_FILE_NAME);
            NetworkService.startActionUpload(mContext, folder + File.separator + LOG_FILE);
        }
        if (spots == null) {
            return;
        }
        Activity activity = (Activity) mContext;
        if (activity == null) {
            return;
        }
    }
}
