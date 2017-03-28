package com.instacart.library.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.instacart.library.truetime.TrueTimeRx;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class Sample2Activity extends AppCompatActivity {

    private static final String TAG = Sample2Activity.class.getSimpleName();
    private static final int COLOR_COUNT = 7;
    private static final int MAX_START_DELAY = 10000;
    private static final int MIN_START_DELAY = 2000;
    private static final int BLINKING_DELAY = 200;
    private static final int BLINKING_DURATION = 5000;
    private static final int COUNTDOWN_TICK = 25;
    private static final int[] RAINBOW_COLORS = new int[] {0xFFFF0000,
            0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00, 0xFF0000FF, 0xFF4B0082, 0xFF8F00FF};

    private static final Handler handler = new Handler();
    private CountDownTimer blinkingTimer;
    private CountDownTimer countdownTimer;
    private int tickCount;

    private Camera camera;
    private Camera.Parameters cameraParams;
    private boolean isFlashOn;

    @Bind(R.id.tt_btn_refresh) Button refreshBtn;
    @Bind(R.id.tt_time_gmt) TextView timeGMT;
    @Bind(R.id.tt_time_pst) TextView timePST;
    @Bind(R.id.tt_time_device) TextView timeDeviceTime;
    @Bind(R.id.activity_main) View mainLayout;
    @Bind(R.id.flash_on_off) CheckBox flashCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_countdown);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        ButterKnife.bind(this);
        refreshBtn.setEnabled(false);
        //initFlashlight();

        //TrueTimeRx.clearCachedInfo(this);

        TrueTimeRx.build()
                .withConnectionTimeout(31_428)
                .withRetryCount(100)
                .withSharedPreferences(this)
                .withLoggingEnabled(true)
                .initializeRx("time.apple.com")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Date>() {
                    @Override
                    public void call(Date date) {
                        //onBtnRefresh();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "something went wrong when trying to initializeRx TrueTime", throwable);
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        refreshBtn.setEnabled(true);
                    }
                });
    }

    @OnClick(R.id.tt_btn_refresh)
    public void onBtnRefresh() {
        if (!TrueTimeRx.isInitialized()) {
            Toast.makeText(this, "Sorry TrueTime not yet initialized.", Toast.LENGTH_SHORT).show();
            return;
        }
        tickCount = 0;

        if (isFlashEnabled()) {
            initFlashlight();
        }

        final Date trueTime = TrueTimeRx.now();
        Date deviceTime = new Date();

        Log.d("kg",
                String.format(" [trueTime: %d] [devicetime: %d] [drift_sec: %f]",
                        trueTime.getTime(),
                        deviceTime.getTime(),
                        (trueTime.getTime() - deviceTime.getTime()) / 1000F));

        /*timeGMT.setText(getString(R.string.tt_time_gmt,
                _formatDate(trueTime, "yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone("GMT"))));
        timePST.setText(getString(R.string.tt_time_pst,
                _formatDate(trueTime, "yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone("GMT-07:00"))));
        timeDeviceTime.setText(getString(R.string.tt_time_device,
                _formatDate(deviceTime,
                        "yyyy-MM-dd HH:mm:ss.SSS",
                        TimeZone.getTimeZone("GMT-07:00"))));*/

        // Blinking

        long startDelay = MIN_START_DELAY + MAX_START_DELAY - (trueTime.getTime() % MAX_START_DELAY);
        Log.i("delay", "startDelay = " + startDelay);
        startCountdown(startDelay);
        scheduleBlinking(trueTime, startDelay);
    }

    private void startCountdown(long startDelay) {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        timeGMT.setVisibility(View.VISIBLE);
        countdownTimer = new CountDownTimer(startDelay, COUNTDOWN_TICK) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeGMT.setText(String.valueOf(millisUntilFinished / 1000 + 1));
            }

            @Override
            public void onFinish() {
                timeGMT.setVisibility(View.INVISIBLE);
            }
        }.start();
    }

    private void scheduleBlinking(final Date trueTime, long startDelay) {
        if (blinkingTimer != null) {
            blinkingTimer.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshBtn.setAlpha(0.2f);
                flashCheckBox.setEnabled(false);
                Log.i("blinkingTimer", "Start Timer : " + trueTime.getTime());
                blinkingTimer = new CountDownTimer(BLINKING_DURATION, BLINKING_DELAY) {

                    public void onTick(long millisUntilFinished) {
                        //Log.i("blinkingTimer", "tick " + tickCount);
                        mainLayout.setBackgroundColor(RAINBOW_COLORS[tickCount % COLOR_COUNT]);
                        if (isFlashEnabled()) {
                            if (tickCount % 2 == 0) {
                                turnOnFlash();
                            } else {
                                turnOffFlash();
                            }
                        }
                        tickCount++;
                    }

                    public void onFinish() {
                        flashCheckBox.setEnabled(true);
                        refreshBtn.setAlpha(1f);
                        if (isFlashEnabled()) {
                            turnOffFlash();
                            if (camera != null) {
                                camera.stopPreview();
                                camera.release();
                                camera = null;
                            }
                        }
                    }
                }.start();
            }
        }, startDelay);
    }

    private String _formatDate(Date date, String pattern, TimeZone timeZone) {
        DateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        format.setTimeZone(timeZone);
        return format.format(date);
    }

    private boolean isFlashAvailable() {
        return getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private boolean isFlashEnabled() {
        return flashCheckBox.isChecked();
    }

    private void initFlashlight() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 50);
        } else {
            if (camera != null) {
                camera.release();
            }
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            try {
                camera.setPreviewTexture(new SurfaceTexture(0));
                camera.startPreview();
                cameraParams = camera.getParameters();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void turnOnFlash() {
        if (!isFlashOn && (camera != null) && (cameraParams != null)) {
            //cameraParams = camera.getParameters();
            cameraParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(cameraParams);
            /*try {
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            isFlashOn = true;
        }
    }

    private void turnOffFlash() {
        if (isFlashOn && (camera != null) && (cameraParams != null)) {
            //cameraParams = camera.getParameters();
            cameraParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(cameraParams);
            /*try {
                camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            isFlashOn = false;
        }
    }
}
