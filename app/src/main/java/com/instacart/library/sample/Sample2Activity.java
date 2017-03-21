package com.instacart.library.sample;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.instacart.library.truetime.TrueTimeRx;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class Sample2Activity
        extends AppCompatActivity {

    private static final String TAG = Sample2Activity.class.getSimpleName();
    private static final Handler handler = new Handler();
    private CountDownTimer timer;
    private int tickCount;

    @Bind(R.id.tt_btn_refresh) Button refreshBtn;
    @Bind(R.id.tt_time_gmt) TextView timeGMT;
    @Bind(R.id.tt_time_pst) TextView timePST;
    @Bind(R.id.tt_time_device) TextView timeDeviceTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        getSupportActionBar().setTitle("TrueTimeRx");

        ButterKnife.bind(this);
        refreshBtn.setEnabled(false);

        //TrueTimeRx.clearCachedInfo(this);

        TrueTimeRx.build()
                .withConnectionTimeout(31_428)
                .withRetryCount(100)
                .withSharedPreferences(this)
                .withLoggingEnabled(true)
                .initializeRx("time.google.com")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Date>() {
                    @Override
                    public void call(Date date) {
                        onBtnRefresh();
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

        final Date trueTime = TrueTimeRx.now();
        Date deviceTime = new Date();

        Log.d("kg",
                String.format(" [trueTime: %d] [devicetime: %d] [drift_sec: %f]",
                        trueTime.getTime(),
                        deviceTime.getTime(),
                        (trueTime.getTime() - deviceTime.getTime()) / 1000F));

        timeGMT.setText(getString(R.string.tt_time_gmt,
                _formatDate(trueTime, "yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone("GMT"))));
        timePST.setText(getString(R.string.tt_time_pst,
                _formatDate(trueTime, "yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone("GMT-07:00"))));
        timeDeviceTime.setText(getString(R.string.tt_time_device,
                _formatDate(deviceTime,
                        "yyyy-MM-dd HH:mm:ss.SSS",
                        TimeZone.getTimeZone("GMT-07:00"))));

        // Blinking

        long startDelay = 1000 - (trueTime.getTime() % 1000);
        Log.i("delay", "startDelay = " + startDelay);

        if (timer != null) {
            timer.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("timer", "Start Timer : " + trueTime.getTime());
                timer = new CountDownTimer(600000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        Log.i("timer", "tick " + tickCount);
                        refreshBtn.setBackgroundColor((tickCount % 2 == 0) ? 0xFF00FF88 : 0xFFFF0088);
                        tickCount++;
                    }

                    public void onFinish() { }
                }.start();
            }
        }, startDelay);
    }

    private String _formatDate(Date date, String pattern, TimeZone timeZone) {
        DateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        format.setTimeZone(timeZone);
        return format.format(date);
    }
}
