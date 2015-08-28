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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;

import uiuc.bioassay.elisa.ELISAApplication;
import uiuc.bioassay.elisa.R;
import uiuc.bioassay.elisa.services.NetworkService;

import static uiuc.bioassay.elisa.ELISAApplication.AVG_FILE_NAME;
import static uiuc.bioassay.elisa.ELISAApplication.processBB;

/**
 * Created by meowle on 7/8/15.
 */
public class BBProcWorker extends AsyncTask<String, Void, Integer> {
    private Context mContext;
    private ProgressDialog progressDialog;
    private String folder;

    public BBProcWorker(Context context) {
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
    protected Integer doInBackground(String... params) {
        folder = params[0];
        return processBB(folder + File.separator);
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
            textView.setText("Error, unable to process data!");
            bbProcActivity.setCurrResult(result);
            return;
        }
        ImageView imageView = (ImageView) bbProcActivity.findViewById(R.id.imageView);
        imageView.setImageBitmap(decodeIMG(folder + File.separator + AVG_FILE_NAME));

        Button button = (Button) bbProcActivity.findViewById(R.id.image_button);
        button.setEnabled(false);
        bbProcActivity.setResult(result);
    }

    private static Bitmap decodeIMG(String img) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeFile(img, options);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
