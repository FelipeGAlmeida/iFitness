package com.personaltrainer.fgapps.personaltrainer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mProxSensor;
    private ProgressDialog progress;
    private MediaPlayer mPlaySuccess;
    private MediaPlayer mPlayFail;

    private Button btn_startstop;
    private ImageButton btn_calib;
    private ImageButton btn_tutorial;
    private TextView txt_dist;
    private TextView txt_quantmeta;
    private SeekBar skb_meta;

    private int last_lum, limiar_dw = 0, meta = 10, cont = 0;
    private int[] lumens_calib;
    private boolean calib = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initInterfaceObjects();
        initProximitySensor();
        initPlayer();
        startCalibration();
    }

    public void initInterfaceObjects(){
        btn_startstop = findViewById(R.id.startstop_id);
        btn_calib = findViewById(R.id.calib_id);
        btn_tutorial = findViewById(R.id.tutorial_id);
        txt_dist = findViewById(R.id.dist_id);
        txt_quantmeta = findViewById(R.id.repmeta_id);
        skb_meta = findViewById(R.id.seekbar_id);

        btn_startstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btn_startstop.getText().equals("Start")) {
                    startSensor();
                    if(mPlayFail.isPlaying()){
                        mPlayFail.stop();
                        initPlayer();
                    }
                } else {
                    stopSensor();
                    mPlayFail.start();
                }
            }
        });
        btn_calib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cont = 0;
                if(mPlayFail.isPlaying()){
                    mPlayFail.stop();
                    initPlayer();
                }
                startCalibration();
            }
        });
        btn_tutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, TutorialActivity.class);
                startActivity(i);
                if(mPlayFail.isPlaying()){
                    mPlayFail.stop();
                    initPlayer();
                }
            }
        });
        skb_meta.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                meta = i+1;
                txt_dist.setText(Integer.toString(meta));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(mPlayFail.isPlaying()){
                    mPlayFail.stop();
                    initPlayer();
                }
                txt_quantmeta.setText("GOAL");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        txt_quantmeta.setText("REPS");
                        if(cont<10) txt_dist.setText("0"+cont);
                        else txt_dist.setText(Integer.toString(cont));
                    }
                }, 1500);
            }
        });
    }

    public void initProximitySensor(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProxSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public void initPlayer(){
        mPlaySuccess = MediaPlayer.create(MainActivity.this, R.raw.birl_source);
        mPlayFail = MediaPlayer.create(MainActivity.this, R.raw.sad_sound);
    }

    private void startCalibration() {
        presentLoading();
        calib = true;
        lumens_calib = new int[10];
        startSensor();
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(calib) {
                    int sum = 0;
                    for (int i = 0; i < cont; i++) {
                        sum += lumens_calib[i];
                    }
                    sum = sum/cont;
                    limiar_dw = (int)(0.6*sum);
                    stopCalibration();
                }
            }
        }, 10000);
    }

    private void stopCalibration() {
        stopSensor();
        calib = false;
        cont = 0;
        dismissLoading();
    }

    private void presentLoading(){
        progress = new ProgressDialog(this);
        progress.setTitle("Calibrating");
        progress.setMessage("Please, wait a while...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
    }

    private void dismissLoading(){
        progress.dismiss();
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float lumens = event.values[0];
        if(calib) {
            lumens_calib[cont] = (int)lumens;
            cont++;
            if(cont>=10){
                int sum = 0;
                for(int i=0;i<lumens_calib.length;i++){
                    sum += lumens_calib[i];
                }
                sum = sum/10;
                limiar_dw = (int)(0.6*sum);
                stopCalibration();
            }

        }else{
            if (last_lum > limiar_dw) {
                if (lumens <= limiar_dw) {
                    cont++;
                    if (cont < 10) txt_dist.setText("0" + cont);
                    else txt_dist.setText(Integer.toString(cont));
                }
            }
            last_lum = (int) lumens;

            if (cont >= meta) {
                mPlaySuccess.start();
                Toast.makeText(MainActivity.this, "Goal accomplished !", Toast.LENGTH_LONG).show();
                stopSensor();
            }
        }
    }

    public void startSensor(){
        mSensorManager.registerListener(this, mProxSensor, SensorManager.SENSOR_DELAY_NORMAL);
        btn_startstop.setText("Stop");
    }

    public void stopSensor(){
        mSensorManager.unregisterListener(this);
        btn_startstop.setText("Start");
        txt_dist.setText("00");
        last_lum = 0;
        cont = 0;
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
    }

}
