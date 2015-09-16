package uiuc.bioassay.elisa.proc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import uiuc.bioassay.elisa.R;

import static uiuc.bioassay.elisa.ELISAApplication.AVG_FILE_NAME;
import static uiuc.bioassay.elisa.ELISAApplication.processBB;
import static uiuc.bioassay.elisa.ELISAApplication.processSample;

/**
 * Created by meowle on 7/8/15.
 */
public class SampleProcWorker extends AsyncTask<String, Void, Integer> {
    private Context mContext;
    private ProgressDialog progressDialog;
    private String folder;

    public SampleProcWorker(Context context) {
        mContext = context;
        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Wait");
        progressDialog.setMessage("Processing Sample...");
        progressDialog.show();
    }

    @Override
    protected Integer doInBackground(String... params) {
        folder = params[0];
        return processSample(folder + File.separator);
    }

    @Override
    protected void onPostExecute(Integer result) {
        progressDialog.dismiss();
        Activity activity = (Activity) mContext;
        SampleProcActivity sampleProcActivity = (SampleProcActivity) activity;
        if (sampleProcActivity == null) {
            return;
        }
        if (result == -1) {
            TextView textView = (TextView) sampleProcActivity.findViewById(R.id.text_result);
            textView.setText("Error, unable to process data!");
            sampleProcActivity.setCurrResult(result);
            return;
        }
        ImageView imageView = (ImageView) sampleProcActivity.findViewById(R.id.imageView);
        imageView.setImageBitmap(decodeIMG(folder + File.separator + "1.jpg"));

        Button button = (Button) sampleProcActivity.findViewById(R.id.image_button);
        button.setEnabled(false);
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
