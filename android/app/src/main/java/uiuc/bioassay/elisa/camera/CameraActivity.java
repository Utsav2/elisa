/**
 * University of Illinois
 Open Source License

 Copyright © <2015>, <University of Illinois at Urbana-Champaign>. All rights reserved.
 All rights reserved.

 Developed by:

 Smartphone Bioassay Team

 University of Illinois at Urbana-Champaign

 http://sb.illinois.edu

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal with
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 of the Software, and to permit persons to whom the Software is furnished to do
 so, subject to the following conditions:

 * Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimers.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimers in the
 documentation and/or other materials provided with the distribution.

 * Neither the names of the Smartphone Bioassay Team, University of Illinois at
 Urbana-Champaign, nor the names of its contributors may be used to
 endorse or promote products derived from this Software without specific
 prior written permission.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE
 SOFTWARE.
 */
package uiuc.bioassay.elisa.camera;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import uiuc.bioassay.elisa.ELISAApplication;
import uiuc.bioassay.elisa.R;
import uiuc.bioassay.elisa.proc.BBProcActivity;
import uiuc.bioassay.elisa.proc.SampleProcActivity;

@SuppressWarnings("deprecation")
public class CameraActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SurfaceHolder.Callback {
    private static final String TAG = "CAMERA";

    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout preview;
    private MediaRecorder mRecorder;
    private Button buttonCapture;
    private String folder;
    private String action;
    private int intExtra;
    private int procMode;
    private String modeExtra;

    private TextView instructions;
    private TextView title;
    private SurfaceView surfaceView;

    private boolean isCapturing = false;
    private int picCount = 0;
    private StartPictureSeriesSound startSeriesSound = new StartPictureSeriesSound();
    private StopPictureSeriesSound stopSeriesSound = new StopPictureSeriesSound();
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error starting preview: " + e.getMessage());
                System.gc();
                mCamera.startPreview();
            }
            File pictureFile = getOutputImageFile(folder, picCount);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.flush();
                fos.close();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(pictureFile)));

                // Take more pictures if still
                ++picCount;
                // TODO: Should I call startPreview again ??? Sometimes it gives me exception
                if (picCount < ELISAApplication.MAX_PICTURE) {
                    mCamera.takePicture(null, null,
                            mPicture);
                } else {
                    stopSeriesSound.play();
                    picCount = 0;
                    buttonCapture.setEnabled(true);
                    setResult(RESULT_OK);
                    if (action.equals(ELISAApplication.ACTION_BROADBAND)) {
                        exportLocationToFile();
                        Intent intent = new Intent(CameraActivity.this, BBProcActivity.class);
                        intent.putExtra(ELISAApplication.MODE_EXTRA, modeExtra);
                        intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder);
                        startActivity(intent);
                    } else if (action.equals(ELISAApplication.ACTION_ONE_SAMPLE)){
                        exportLocationToFile();
                        Intent intent = new Intent(CameraActivity.this, SampleProcActivity.class);
                        intent.setAction(ELISAApplication.ACTION_ONE_SAMPLE);
                        intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder);
                        startActivity(intent);
                    } else if (action.equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
                        exportLocationToFile();
                        Intent intent = new Intent(CameraActivity.this, SampleProcActivity.class);
                        intent.setAction(ELISAApplication.ACTION_MULTIPLE_SAMPLE);
                        intent.putExtra(ELISAApplication.FOLDER_EXTRA, folder);
                        Log.d(TAG, "intExtra: " + intExtra);
                        intent.putExtra(ELISAApplication.INT_EXTRA, intExtra);
                        intent.putExtra(ELISAApplication.ELISA_PROC_MODE, procMode);
                        startActivity(intent);
                    }
                    finish();
                }
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            } catch (Exception e) {
                System.gc();
                mCamera.takePicture(null, null,
                        mPicture);
            }
        }
    };


    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 20000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;


    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        folder = getIntent().getStringExtra(ELISAApplication.FOLDER_EXTRA);
        action = getIntent().getAction();
        intExtra = getIntent().getIntExtra(ELISAApplication.INT_EXTRA, -1);
        procMode = getIntent().getIntExtra(ELISAApplication.ELISA_PROC_MODE, 0);
        modeExtra = getIntent().getStringExtra(ELISAApplication.MODE_EXTRA);
        Log.d(TAG, folder);
        Log.d(TAG, action);

        final boolean isFluorescent = modeExtra.equals(ELISAApplication.MODE_FLUORESCENT);
        surfaceView = (SurfaceView)findViewById(R.id.surface_camera);
        surfaceView.setEnabled(false);
        surfaceView.getHolder().addCallback(this);
        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Open camera
        openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);

        // Set onClickListener for taking picture
        buttonCapture = (Button) findViewById(R.id.button_capture);
        buttonCapture.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isCapturing) {
                            buttonCapture.setText(R.string.capture);
                            isCapturing = false;
                            stopRecording();
                        }
                        else if (isFluorescent) {
                            buttonCapture.setText(R.string.stop_capturing);
                            isCapturing = true;
                            startRecording();
                        } else {
                            buttonCapture.setEnabled(false);
                            startSeriesSound.play();
                            mCamera.takePicture(null, null, mPicture);
                        }
                    }
                }
        );

        TouchRectView touchRectView = (TouchRectView) findViewById(R.id.touch_rect);
        mPreview.setTouchRectView(touchRectView);

        instructions = (TextView) findViewById(R.id.instructions);
        title = (TextView) findViewById(R.id.camera_title);

        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();
    }

    protected void startRecording() {
        releaseCamera();
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        mCamera.unlock();

        mRecorder = new MediaRecorder();  // Works well

        mRecorder.setCamera(mCamera);

        mRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        File videoFile = getOutputVideoFile(folder);
        if (videoFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return;
        }
        mRecorder.setOutputFile(videoFile.getAbsolutePath());
        Log.d(TAG, "Saving video at: " + videoFile.getAbsolutePath());

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Exception in trying to prepare mediaRecorder", e);
        }
        mRecorder.start();
    }

    protected void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        releaseCamera();
    }

    private void releaseMediaRecorder(){
        if (mRecorder != null) {
            mRecorder.reset();   // clear recorder configuration
            mRecorder.release(); // release the recorder object
            mRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    /*
    protected void onNewIntent (Intent intent) {
        folder = intent.getStringExtra(ELISAApplication.FOLDER_EXTRA);
        action = intent.getAction();
        intExtra = intent.getIntExtra(ELISAApplication.INT_EXTRA, -1);
        procMode = intent.getIntExtra(ELISAApplication.ELISA_PROC_MODE, 0);
    }*/

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }


    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }

        // Start update location
        startLocationUpdates();
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }

        if (mCamera == null) {
            openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        if (action.equals(ELISAApplication.ACTION_BROADBAND)) {
            title.setText("Broadband Screen");
        } else if (action.equals(ELISAApplication.ACTION_ONE_SAMPLE) || action.equals(ELISAApplication.ACTION_MULTIPLE_SAMPLE)) {
            title.setText("Sample Screen");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // release the elisa immediately on pause event
        releaseCamera();

        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
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

    @Override
    public void onBackPressed() {
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.stopPreview();
            //mCamera.setPreviewCallback(null);
            if (mPreview != null) {
                mPreview.getHolder().removeCallback(mPreview);
            }
            if (preview != null) {
                preview.removeView(mPreview);
            }
            mCamera.release();        // release the elisa for other applications
            mPreview = null;
            mCamera = null;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    private static Camera getCameraInstance(int cameraId){
        Log.d(TAG, "Attempting to get camera instance");
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG, "", e);
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if elisa is unavailable
    }

    /** Set elisa display orientation */
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /** Open elisa */
    private void openCamera(int cameraId) {
        if (mCamera != null) {
            return;
        }
        /* TODO: If Camera Activity is called from another activity, return error if cannot open */
        mCamera = getCameraInstance(cameraId);
        if (mCamera == null) {
            Log.d(TAG, "Camera is null");
            return;
        }

        // Set default elisa parameters using Read, Modify, Write technique
        // Read
        Camera.Parameters params = mCamera.getParameters();

        // Modify
        // Set elisa orientation
        setCameraDisplayOrientation(this, cameraId, mCamera);
        // Search for best preview size and set preview size
        {
            int width = 0;
            int height = 0;
            int maxArea = 0;
            for (Camera.Size size : params.getSupportedPreviewSizes()) {
                int area = size.width * size.height;
                if (area > maxArea) {
                    width = size.width;
                    height = size.height;
                    maxArea = area;
                }
            }
            params.setPreviewSize(width, height);
        }

        // Search for best picture size and set piture size
        {
            int width = 0;
            int height = 0;
            int maxArea = 0;
            for (Camera.Size size : params.getSupportedPictureSizes ()) {
                int area = size.width * size.height;
                if (area > maxArea) {
                    width = size.width;
                    height = size.height;
                    maxArea = area;
                }
            }
            params.setPictureSize(width, height);
        }

        // TODO: may want to enable this to view paramerater that aren't exposed by the API
        // Log.d(TAG, params.flatten());

        // Set metering mode
        params.set("metering", "matrix");

        // Set anti banding
        params.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);

        // Set image quality
        params.setJpegQuality(100);

        // Set focus mode
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);

        // Set exposure offset
        params.setExposureCompensation(0);


        if (modeExtra.equals(ELISAApplication.MODE_FLUORESCENT)) {
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
            // Set flash
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            // Set iso
            params.set("iso", String.valueOf("800"));
        } else {
            // Set white balance
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
            // Set flash
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            // Set iso
            params.set("iso", String.valueOf("100"));
        }

        // lock auto exposure
        if (params.isAutoExposureLockSupported()) {
           params.setAutoExposureLock(true);
        }

        // lock auto white balance
        if (params.isAutoWhiteBalanceLockSupported()) {
            params.setAutoWhiteBalanceLock(true);
        }

        // Write
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);

        mPreview.setFocusOnTouch(false);

        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    /** Shutter Start Series Sound */
    private static class StartPictureSeriesSound {
        private final MediaActionSound media;

        private StartPictureSeriesSound() {
            media = new MediaActionSound();
            media.load(MediaActionSound.START_VIDEO_RECORDING);
        }

        public void play() {
            media.play(MediaActionSound.START_VIDEO_RECORDING);
        }
    }

    /** Shutter Stop Series Sound */
    private static class StopPictureSeriesSound {
        private final MediaActionSound media;

        private StopPictureSeriesSound() {
            media = new MediaActionSound();
            media.load(MediaActionSound.STOP_VIDEO_RECORDING);
        }

        public void play() {
            media.play(MediaActionSound.STOP_VIDEO_RECORDING);
        }
    }

    private static File createDirectory(String folder) {
        File mediaStorageDir = new File(folder);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }
        return mediaStorageDir;
    }

    private static File getOutputVideoFile(String folder) {
        File mediaStorageDir = createDirectory(folder);
        if (mediaStorageDir == null) {
            return null;
        }
        return new File(mediaStorageDir.getPath() + File.separator + "video.mp4");
    }

    /** Create a File for saving an image */
    private static File getOutputImageFile(String folder, int count){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = createDirectory(folder);
        if (mediaStorageDir == null) {
            return null;
        }
        // Create a media file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                (count  + 1) + ".jpg");
    }

    private void exportLocationToFile() {
        //Toast.makeText(this, "Location updated. Latitude: " + mCurrentLocation.getLatitude() + ", longitude: " + mCurrentLocation.getLongitude() + ", time: " + mLastUpdateTime,
        //        Toast.LENGTH_LONG).show();
        BufferedWriter out = null;
        try {
            FileWriter fstream = new FileWriter(folder + File.separator + ELISAApplication.LOG_FILE, true); //true tells to append data.
            out = new BufferedWriter(fstream);
            if (mCurrentLocation == null) {
                out.write("Location: unknown");
            } else {
                out.write("Location: \n\tLatitude:" + mCurrentLocation.getLatitude() + "\n\tLongitude: " + mCurrentLocation.getLongitude() + "\n\tTime: " + mLastUpdateTime + "\n");
            }
            out.flush();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error: " + e.getMessage());
        }
        finally
        {
            if(out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
