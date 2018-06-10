package pl.agh.roadsigns.camera2detector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import imgProcessing.activity.ProceedImg;
import imgProcessing.config.Config;

public class Camera2DetectorActivity extends AppCompatActivity {

    private  static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private  static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 0;
    private  static final int STATE_PREVIEW = 0;
    private  static final int STATE_WAIT_LOCK = 1;
    private int mCaptureState = STATE_PREVIEW;

    private RequestQueue locationRequestQueue;
    private boolean initLocation = true;
    private volatile String userAddress;

    private TextToSpeech toSpeech;
    private int langTextToSpeechResult;

    private MediaPlayer circleDetectedSoun;
    private MediaPlayer redAreaDetectedSoun;

    private TextView mTextViewPrevRes;
    private TextView mTextViewCoordinates;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private ImageButton btnSettings;
    private ImageButton btnShotPhoto;
    private ImageButton btnDetector;

    private static List<Size> previewResolutions = new ArrayList<Size>();
    private static boolean isAcitve = false;
    private static boolean isDetectorActive = false;

    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListeenr = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            //toSpeech.speak("My TextureVIew is available with resolution " + width + " width and " + height +" height", TextToSpeech.QUEUE_ADD,null,null);
            toSpeech.speak("Hello, my TextureVIew is available", TextToSpeech.QUEUE_ADD,null,null);
            Toast.makeText(getApplicationContext(), "TextureVIew is available width:" + width + " height:" + height, Toast.LENGTH_LONG).show();
            setupCamera(width, height);
            connectToCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if((!isAcitve)&&(isDetectorActive))
                lockFocus();

        }

    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            Toast.makeText(getApplicationContext(), "Camera connection made!!", Toast.LENGTH_SHORT).show();
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;

        }
    };

    private HandlerThread mBackgroundHandlerThreadFirst;

    private Handler mBackgroundHandlerFirst;

    private String mCameraID;
    private Size mPreviewSize;

    private int starts = 1;

    private Size mImageSize;
    private ImageReader mImageReade;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener1 = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            starts++;
            if(((starts%2)==0)&&(!isAcitve)) {
                mBackgroundHandlerFirst.post(new ImageSaver(1, reader.acquireLatestImage()));
            }
        }
    };

    private class ImageSaver implements Runnable{

        private final Image mImage;
        private  int id;

        public ImageSaver(int id, Image image) {
            mImage = image;
            this.id = id;
        }

        @Override
        public void run() {
            //System.out.println("Action");
            isAcitve = true;
            if(mImage!=null){
                ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();

                byte[] bytes = new byte[byteBuffer.remaining()];

                byteBuffer.get(bytes);

                ArrayList<String> detected = ProceedImg.detectSigns(bytes, getApplicationContext(), mImageFolder, toSpeech, circleDetectedSoun, redAreaDetectedSoun);
/*
                if(detected.size()==0){
                    toSpeech.speak("I'm still looking for signs!", TextToSpeech.QUEUE_ADD,null,null);
                }*/

                for(String s : detected){
                    toSpeech.speak("I detected and found "+ s+" speed limit", TextToSpeech.QUEUE_ADD,null,null);
                    Toast.makeText(getApplicationContext(),"Detecting Signs: I found "+ s + " speed limit", Toast.LENGTH_SHORT).show();
                }
                mImage.close();
                isAcitve = false;
            }
            isAcitve = false;
        }
    }
    private int mTotalRotation;
    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private  void process(CaptureResult captureResult){
            switch (mCaptureState){
                case STATE_PREVIEW:
                    //do nothing
                    break;
                case STATE_WAIT_LOCK:
                    mCaptureState = STATE_PREVIEW;
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if((afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)||(afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)){

                        //toSpeech.speak("AutoFocus locked!", TextToSpeech.QUEUE_ADD,null,null);
                        //Toast.makeText(getApplicationContext(),"AutoFocus is locked", Toast.LENGTH_SHORT).show();
                        startStillCaptureREquest();
                    }
                    break;
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }
    };

    private CaptureRequest.Builder mCaptureRequestBuilder;

    private File mImageFolder;
    private String mImageFileName;

    //translating device otientation numbers into real world degrees
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getHeight() * o1.getWidth() / (long) o2.getHeight() * o2.getWidth());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_detector);

        this.createImageFolder();

        Config.prepare(getApplicationContext().getResources());

        checkWriteStoragePermission();

        this.locationRequestQueue = Volley.newRequestQueue(this);

        this.circleDetectedSoun = MediaPlayer.create(this,R.raw.bleep);

        this.redAreaDetectedSoun = MediaPlayer.create(this, R.raw.circle_detected);

        this.toSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    langTextToSpeechResult = toSpeech.setLanguage(Locale.ENGLISH);
                }else{
                    Toast.makeText(getApplicationContext(),"Text to Speech not supported", Toast.LENGTH_SHORT).show();
                }
            }
        });

        this.mTextureView = (TextureView) findViewById(R.id.textureView);

        this.mTextViewPrevRes = (TextView) findViewById(R.id.textViewPrevRes);

        this.mTextViewCoordinates = (TextView) findViewById(R.id.coordinates);

        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        this.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                mTextViewCoordinates.setText("Coordinates: long: " + location.getLongitude() + "  lat: " + location.getLatitude() + "\n "+ userAddress);

                if(initLocation || Config.locationRequest){
                    String url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                            "latlng=" + location.getLatitude() + "," + location.getLongitude() +"&key="+Config.GPS_GOOGLEMAPS_API;

                    JsonObjectRequest request = new JsonObjectRequest(url,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {

                                    try {
                                        userAddress = response.getJSONArray("results").getJSONObject(0).getString("formatted_address");
                                        Config.GPS_GEOCODDED_LOCATION_RESPONSES.add(userAddress);
                                        //System.out.println(userAddress);
                                        Config.locationRequest = false;
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Toast.makeText(getApplicationContext(), "Error with geocode api "+error.networkResponse.statusCode,Toast.LENGTH_LONG).show();
                                }
                            });
                    locationRequestQueue.add(request);
                    initLocation = false;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        setupGPS();

        this.btnSettings = (ImageButton) findViewById(R.id.settingsButton);

        this.btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final PopupMenu popupMenu = new PopupMenu(getApplicationContext(), btnSettings);
                String[] a = {"a", "b", "c"};
                popupMenu.getMenu().addSubMenu("Set preview resolution");
                for(Size s: previewResolutions){
                    popupMenu.getMenu().getItem(0).getSubMenu().add(s.toString());
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.toString().contains("x")){ //prev res options
                            String[] measures = item.toString().split("x");

                            mPreviewSize = new Size(Integer.parseInt(measures[0]), Integer.parseInt(measures[1]));
                            startPreview();
                            mTextViewPrevRes.setText("preview resolution: "+ mPreviewSize);
                            Toast.makeText(getApplicationContext(), "Preview resolution "+mPreviewSize+" chosen",Toast.LENGTH_LONG).show();
                        }

                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        this.btnShotPhoto = (ImageButton) findViewById(R.id.photo_det);
        this.btnShotPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isDetectorActive) {
                    toSpeech.speak("Detector was deactivated", TextToSpeech.QUEUE_ADD, null, null);
                    isDetectorActive = !isDetectorActive;
                }
                toSpeech.speak("Shoot on demand!", TextToSpeech.QUEUE_ADD, null, null);
                lockFocus();
            }
        });

        this.btnDetector = (ImageButton) findViewById(R.id.constant_det_off);
        this.btnDetector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isDetectorActive) {
                    toSpeech.speak("Detector was deactivated", TextToSpeech.QUEUE_ADD, null, null);
                }else {
                    toSpeech.speak("Detector was activated, I'm looking for signs", TextToSpeech.QUEUE_ADD, null, null);
                }
                isDetectorActive = !isDetectorActive;
            }
        });
    }
    private void setupGPS(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,10);
            }
            return;
        }
        this.locationManager.requestLocationUpdates("gps",Config.GPS_REFRESH_INTERVAL,Config.GPS_REFRESH_CHANGE_DISTANCE_METERS, locationListener);
        toSpeech.speak("GPS was configured", TextToSpeech.QUEUE_ADD,null,null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.startBackgroundThread();

        if (this.mTextureView.isAvailable()) {
            setupCamera(this.mTextureView.getWidth(), this.mTextureView.getHeight());
            connectToCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListeenr);
        }
    }

    @Override
    protected void onPause() {
        this.closeCamera();
        this.stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //traversing through camera ids
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //checking if we are in portrait (or landscape) mode to switch height with width
                int deviceOreintation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation  = sensorToDEviceRotations(cameraCharacteristics, deviceOreintation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                this.mPreviewSize = chooseOoptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                this.mImageSize = chooseOoptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                this.mImageReade = ImageReader.newInstance(this.mImageSize.getWidth(), this.mImageSize.getHeight(), ImageFormat.JPEG, 2);
                this.mImageReade.setOnImageAvailableListener(this.mOnImageAvailableListener1, mBackgroundHandlerFirst);

                this.mTextViewPrevRes.setText("preview resolution: "+ this.mPreviewSize);
                this.mCameraID = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectToCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){  //if greater or equal to marshmallow
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    cameraManager.openCamera(this.mCameraID, this.mCameraDeviceStateCallback, this.mBackgroundHandlerFirst);
                }else{
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this, "Our app requires access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            }else{
                cameraManager.openCamera(this.mCameraID, this.mCameraDeviceStateCallback, this.mBackgroundHandlerFirst);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        //getting texture view and converting into a surface view that the camera API understands -feeding camera sensor
        SurfaceTexture surfaceTexture = this.mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            this.mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            this.mCaptureRequestBuilder.addTarget(previewSurface);

            this.mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, this.mImageReade.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mPreviewCaptureSession = session;
                    try {
                        mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandlerFirst); //callback to processing data?
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Something went wrong with setting camera preview", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startStillCaptureREquest(){
        try {
            this.mCaptureRequestBuilder = this.mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            this.mCaptureRequestBuilder.addTarget((this.mImageReade.getSurface()));
            this.mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);
            CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    /*
                    try {
                        createImageFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                }
            };
            this.mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(),  "Application needs camera services!", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == 10){
            setupGPS();
        }
    }

    //free camera resources
    private  void closeCamera(){
        if(this.mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
    //setting up background thread
    private void startBackgroundThread(){
        this.mBackgroundHandlerThreadFirst = new HandlerThread("AndroidProject1");
        this.mBackgroundHandlerThreadFirst.start();
        mBackgroundHandlerFirst = new Handler(this.mBackgroundHandlerThreadFirst.getLooper());
    }

    private void stopBackgroundThread(){

        this.mBackgroundHandlerThreadFirst.quitSafely();
        try {
            this.mBackgroundHandlerThreadFirst.join();
            this.mBackgroundHandlerThreadFirst = null;
            this.mBackgroundHandlerFirst = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDEviceRotations(CameraCharacteristics cameraCharacteristics, int deviceOrientation){
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOoptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<Size>();
        //traverse through resolutions
        for(Size option : choices) {
            previewResolutions.add(option);
            bigEnough.add(option);
            if(option.getWidth() == 720)  // taking 720x480
                return option;
        }
        return choices[0];
    }

    private void lockFocus(){

        this.mCaptureState = STATE_WAIT_LOCK;
        this.mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            this.mPreviewCaptureSession.capture(this.mCaptureRequestBuilder.build(), mPreviewCaptureCallback, this.mBackgroundHandlerFirst);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createImageFolder(){
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        this.mImageFolder = new File(imageFile, "AndroidProject");
        if(!this.mImageFolder.exists()){
            this.mImageFolder.mkdirs();
        }
    }

    private File createImageFileName() throws IOException{
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prefix = "Shot_"+ timestamp+"_";
        File imageFile = File.createTempFile(prefix,".jpg",this.mImageFolder);
        Toast.makeText(getApplicationContext(),"Saving shot!", Toast.LENGTH_SHORT).show();
        this.mImageFileName = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void checkWriteStoragePermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){

            }else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(this, "app needs perms to save pics", Toast.LENGTH_LONG);
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        }
    }
}
