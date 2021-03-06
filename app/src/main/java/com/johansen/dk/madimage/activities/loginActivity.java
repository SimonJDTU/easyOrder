package com.johansen.dk.madimage.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.johansen.dk.madimage.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import io.fabric.sdk.android.Fabric;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class loginActivity extends AppCompatActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{

    private ImageButton danishFlag, englishFlag, turkishFlag;
    private SurfaceView cameraPreview;
    private CameraSource cameraSrc;
    private BarcodeDetector barcodeDetector;
    private TextView loginInfo;
    private Button helpBtn;
    private final Context context = this;
    private long time = 0;
    private SharedPreferences prefs = null;
    private Typeface tf;
    private Vibrator vibe;
    private ImageView moveAlongBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        if (isTablet()) {
            setContentView(R.layout.activity_login_tablet);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setContentView(R.layout.activity_login);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        loginInfo = findViewById(R.id.explqr);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(loginInfo, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        helpBtn = findViewById(R.id.helpBtn);
        moveAlongBtn = findViewById(R.id.easyOrderLogo);
        tf = Typeface.createFromAsset(getAssets(), "fonts/Orkney Regular.ttf");

        /*using fonts on text fields*/
        loginInfo.setTypeface(tf);
        helpBtn.setTypeface(tf);
        helpBtn.setTypeface(tf);

        /*defining elements for QR-scanner*/
        //from youtubevideo: https://www.youtube.com/watch?v=ej51mAYXbKs
        cameraPreview = findViewById(R.id.cameraPreview);
        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSrc = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(1024, 768)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .setAutoFocusEnabled(true)
                .build();

        /*defining image Buttons*/
        danishFlag = findViewById(R.id.danishFlag);
        englishFlag = findViewById(R.id.englishFlag);
        turkishFlag = findViewById(R.id.turkishFlag);

        /*OnClick listener on Buttons*/
        moveAlongBtn.setOnClickListener(this);
        helpBtn.setOnClickListener(this);
        danishFlag.setOnClickListener(this);
        englishFlag.setOnClickListener(this);
        turkishFlag.setOnClickListener(this);
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if(!isNetworkAvailable()){
            //NO INTERNET
            //Todo: Handle this event better. Either deny access to app or guide the user to whatever could be wrong.
             Toast.makeText(this,"No internet connection was detected",Toast.LENGTH_LONG).show();
        }
        setLocale();
    }

    @Override
    public void onClick(View v) {
        vibe.vibrate(100);
        /*Implementing on click listener to QR-code image Button*/
        switch (v.getId()) {
            case R.id.easyOrderLogo:
                if(isTablet()){
                    startActivity(new Intent(loginActivity.this, selectionActivity_tablet.class));
                } else {
                    startActivity(new Intent(loginActivity.this, selectionActivity.class));
                }
                break;
            case R.id.helpBtn:
                // get prompts.xml view
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.help, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);
                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);
                // set dialog message
                alertDialogBuilder
                        .setCancelable(true)
                        .setPositiveButton("OK",
                                (dialog, id) -> dialog.cancel());
                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                // show it
                alertDialog.show();
                break;
            case R.id.danishFlag:
                prefs = getSharedPreferences("setLanguage", MODE_PRIVATE);
                prefs.edit().putString("language", "da").apply();
                setLocale();
                break;
            case R.id.englishFlag:
                prefs = getSharedPreferences("setLanguage", MODE_PRIVATE);
                prefs.edit().putString("language", "en").apply();
                setLocale();
                break;
            case R.id.turkishFlag:
                prefs = getSharedPreferences("setLanguage", MODE_PRIVATE);
                prefs.edit().putString("language", "tr").apply();
                setLocale();
                break;
            default:
                Toast.makeText(this, "DEFAULT HIT IN ONCLICK SWITCH", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private boolean timeAllowedBetweenScans() {
        long currentTime = System.currentTimeMillis();
        return currentTime - time > 3000;
    }

    private void createQRscan() {
        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                if(!(Build.VERSION.SDK_INT <= LOLLIPOP)) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        askPermission();
                        return;
                    }
                }
                try {
                    cameraSrc.start(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSrc.stop();
            }
        });
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();

                if (qrCodes.size() != 0) {
                    loginInfo.post(() -> {
                        if (timeAllowedBetweenScans()) {
                            time = System.currentTimeMillis();
                            if (qrRegex(qrCodes.valueAt(0).displayValue)) {

                                vibe.vibrate(200);
                                Intent intent;
                                if(isTablet()){
                                    intent = new Intent(loginActivity.this, selectionActivity_tablet.class);
                                } else {
                                    intent = new Intent(loginActivity.this, selectionActivity.class);
                                }
                                intent.putExtra("roomNo", qrCodes.valueAt(0).displayValue);
                                startActivity(intent);
                            } else {
                                Toast.makeText(getApplicationContext(),"Couldn't recognize QR-code, try again", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }
            }
        });
    }

    //this method of checking screensize is from stackoverflow: https://stackoverflow.com/questions/9279111/determine-if-the-device-is-a-smartphone-or-tablet
    private boolean isTablet() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float yInches = metrics.heightPixels / metrics.ydpi;
        float xInches = metrics.widthPixels / metrics.xdpi;

        return Math.sqrt(xInches * xInches + yInches * yInches) >= 6.5;
    }

    protected void onResume() {
        super.onResume();
        createQRscan();
    }

    private void setLocale() {
        prefs = getSharedPreferences("setLanguage", MODE_PRIVATE);
        String lang = prefs.getString("language", Locale.getDefault().getDisplayLanguage());
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);

        helpBtn.setText(getString(R.string.help_button_text));
        loginInfo.setText(getString(R.string.login_info));
    }

    // Must be done during an initialization phase like onCreate
    private void askPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        recreate();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if(response.isPermanentlyDenied()) {
                            //TODO: Handle denial efficiently
                        } else{

                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private boolean qrRegex(String codeString) {

        // 6-14 character password requiring numbers and alphabets of both cases
        final String PASSWORD_REGEX =
                "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{16}$";

        /* 4-32 character password requiring at least 3 out of 4 (uppercase
        // and lowercase letters, numbers & special characters) and at-most
        // 2 equal consecutive chars.
        final String COMPLEX_PASSWORD_REGEX =
                "^(?:(?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|" +
                        "(?=.*\\d)(?=.*[^A-Za-z0-9])(?=.*[a-z])|" +
                        "(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|" +
                        "(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))(?!.*(.)\\1{2,})" +
                        "[A-Za-z0-9!~<>,;:_=?*+#.\"&§%°()\\|\\[\\]\\-\\$\\^\\@\\/]" +
                        "{8,32}$";
        */

        final Pattern PASSWORD_PATTERN =
                Pattern.compile(PASSWORD_REGEX);

        return PASSWORD_PATTERN.matcher(codeString).matches();
    }

}