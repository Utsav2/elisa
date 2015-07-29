package uiuc.bioassay.elisa;

import android.app.Activity;
import android.app.Application;
import android.os.Environment;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by meowle on 7/1/15.
 */
public class ELISAApplication extends Application {
    static {
        System.loadLibrary("elisa");
    }

    public static final String FOLDER_EXTRA = "FOLDER_EXTRA";
    public static final String ROOT_FOLDER = Environment.getExternalStorageDirectory() + "/Android/data/uiuc.bioassay.elisa";
    public static final String ACTION_BROADBAND = "BROADBAND";

    public static double RED_LASER_PEAK = 1016.037828;
    public static double GREEN_LASER_PEAK = 1443.327951;
    public static final double RED_LASER_NM = 656.26;
    public static final double GREEN_LASER_NM = 532.1;



    /*----------------------------------------------------------------------------*/
    // If you change the following constants, remmember to change in elisa.h as well
    public static final String AVG_FILE_NAME = "avg.jpg";
    public static final String LOG_FILE = "log.txt";
    public static final String BB_FOLDER = "bg/";
    public static final String SAMPLE_FOLDER = "sample/";
    public static final int MAX_PICTURE = 8;
    /*----------------------------------------------------------------------------*/

    public static final String PILLS_FOLDER = "pills";

    /* Some helper functions */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /* Native signatures */
    public native static void cleanFolder(String folder);
    public native static double[] processBBELISA(String folder);
    public native static double[] processSampleELISA(String folder);
}
