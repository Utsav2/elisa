package uiuc.bioassay.elisa;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.support.multidex.MultiDex;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by meowle on 7/1/15.
 */
public class ELISAApplication extends Application {
    static {
        System.loadLibrary("elisa");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static final String FOLDER_EXTRA = "FOLDER_EXTRA";
    public static final String INT_EXTRA = "INT_EXTRA";;
    public static final String NUM_STDS = "NUM_STDS";
    public static final String MAX_NUM_REPLICATES = "NUM_REPLICATES";
    public static final String ROOT_FOLDER = Environment.getExternalStorageDirectory() + "/Android/data/uiuc.bioassay.elisa";
    public static final String ACTION_BROADBAND = "BROADBAND";
    public static final String ACTION_ONE_SAMPLE = "ONE_SAMPLE";
    public static final String ACTION_MULTIPLE_SAMPLE = "MULTIPLE_SAMPLE";
    public static final String MODE_EXTRA = "MODE_EXTRA";
    public static final String VIDEO_EXTRA = "VIDEO_EXTRA";
    public static final String MODE_ABSORPTION = "ABSORPTION";
    public static final String MODE_FLUORESCENT = "FLUORESCENT";
    public static final String MODE_ELISA = "ELISA";

    public static double RED_LASER_PEAK = 1016.037828;
    public static double GREEN_LASER_PEAK = 1443.327951;
    public static final double RED_LASER_NM = 656.26;
    public static final double GREEN_LASER_NM = 532.1;

    public static int currentSampleIdx;
    public static double resultSampleAbs;
    public static double [] currentNormalizedAreas;
    public static final String ELISA_PROC_MODE = "ELISA_PROC_MODE";
    public static final String ELISA_ABS_RESULT = "ELISA_ABS_RESULT";
    public static final String ELISA_STDS = "ELISA_STDS";
    public static final int ELISA_PROC_MODE_450nm = 0;
    public static final int ELISA_PROC_MODE_FULL_INTEGRATION = 1;
    public static final double FLUOROSCENT_FRAME_AREA_THRESHOLD = 0.2;
    public static final double FLUOROSCENT_LASER_AND_DATA_AREA_THRESHOLD = 0.2;
    public static final String NUM_PEAKS = "NUM_PEAKS";
    public static final String SAVE_TO_DISK= "SAVE_TO_DISK";


    /*----------------------------------------------------------------------------*/
    // If you change the following constants, remmember to change in elisa.h as well
    public static final String AVG_FILE_NAME = "image.jpg";
    public static final String LOG_FILE = "log.txt";
    public static final String BB_FOLDER = "bb";
    public static final String SAMPLE_FOLDER = "sample";
    public static final String RES = "res.bin";
    public static final String RGB_SPEC = "rgb.bin";
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
    public native static int processVideo(String videoFile);
    public native static int processBB(String folder);
    public native static int processF(String folder);
    public native static int processSample(String folder, int action);
    public native static double[] readRGBSpec(String folder);
    public native static double[] readBBResNormalized(String folder);
    public native static double[] readSampleResNormalized(String folder);
}
