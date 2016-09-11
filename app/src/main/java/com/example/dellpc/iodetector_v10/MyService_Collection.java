package com.example.dellpc.iodetector_v10;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import static java.lang.Thread.sleep;

public class MyService_Collection extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public final String TAG = "MyService_Collection";
    private IBinder binder_service;
    String[] headers_c1, headers_c2;
    String[] csv_1 = new String[5];
    String[] csv_2 = new String[4];
    String csv_file_name_1;
    String csv_file_name_2;
    CSVWriter writer1 = null;
    CSVWriter writer2 = null;
    GeomagneticField geoField;
    TelephonyManager mTelephonyManager;
    MyPhoneStateListener mPhoneStatelistener;
    int mSignalStrength = 0;
    SensorManager sensorManager;
    Sensor light_sensor;
    SensorEventListener lightEventListener;
    Sensor prox_sensor;
    SensorEventListener proxEventListener;
    Sensor mag_sensor;
    BroadcastReceiver mBatInfoReceiver;
    SoundMeter sm;
    GoogleApiClient mGoogleApiClient;
    Location mLocation;
    Thread t_light,t_proximity;
    float light_val;
    float prox_value;

    public MyService_Collection() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ON CREATE");

        sm = new SoundMeter();
       //building google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        csv_file_name_1 = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/class_1" + ".csv";
        csv_file_name_2 = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/class_2" + ".csv";
        headers_c1 = new String[]{"Cell_Signal_Strength", "Light_Intensity", "Time", "Proximity_val","Label"};
        headers_c2 = new String[]{"Battery temp", "sound_amp", "magnetic_variance","Label"};
        mPhoneStatelistener = new MyPhoneStateListener();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        light_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        prox_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mag_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Log.d(TAG, "Light Sensor:" + light_sensor);
        Log.d(TAG, "Prox Sensor:" + light_sensor);
        //Gives light sensor values
        t_light=new Thread(new Runnable() {
            @Override
            public void run() {
                while (lightEventListener == null) {
                    lightEventListener = new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            light_val = event.values[0];
                            System.out.println("Sending light:" + light_val);
                            //Added Light sensor value
                            csv_1[1]=""+light_val;
                            sensorManager.unregisterListener(lightEventListener, light_sensor);
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {

                        }
                    };

                }
                sensorManager.registerListener(lightEventListener, light_sensor, SensorManager.SENSOR_DELAY_NORMAL);

            }
        });
        //Gives proximity values
        t_proximity=new Thread(new Runnable() {
            @Override
            public void run() {
                while (proxEventListener == null) {
                    proxEventListener = new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            prox_value=event.values[0];
                            csv_1[3]=""+prox_value;
                            sensorManager.unregisterListener(proxEventListener, prox_sensor);

                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {

                        }
                    };
                }
                sensorManager.registerListener(proxEventListener, prox_sensor, SensorManager.SENSOR_DELAY_NORMAL);

            }
        });

    }


    @Override
    public IBinder onBind(Intent intent) {
        System.err.println("On Bind");
        Arrays.fill(csv_1,"");
        Arrays.fill(csv_2,"");
        t_light.start();
        t_proximity.start();
        mGoogleApiClient.connect();
        Log.d(TAG, "Inside onBind of service");
        writer1 = generateWriterHeadings(writer1, csv_file_name_1, headers_c1);
        writer2 = generateWriterHeadings(writer2, csv_file_name_2, headers_c2);
        sensorManager.registerListener(lightEventListener, light_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(proxEventListener, prox_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        /////////////////////////////////////////////////////////////////////////////////////////////////
        //Adding Signal Strength
        mPhoneStatelistener = new MyPhoneStateListener();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        //Added System Time
        csv_1[2] = "" + (System.currentTimeMillis());

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                // TODO Auto-generated method stub
                int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
                //Added Battery temperature
                csv_2[0] = Integer.toString(temp);

            }
        };
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //Added signal Strength
        csv_2[1] = Double.toString(getAmp(sm) / 10);
        Log.d(TAG,"AMP= "+csv_2[1]);

        Toast.makeText(this.getBaseContext(),"Press Stop Button after 3secs",Toast.LENGTH_SHORT).show();

        return binder_service;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"Enter onUnBind of MyService_Collection");
        mGoogleApiClient.disconnect();
        csv_1[4]=MainActivity.label;
        csv_2[3]=MainActivity.label;
        writer1.writeNext(csv_1);
        try {
            writer1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer2.writeNext(csv_2);
        try {
            writer2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sensorManager.unregisterListener(proxEventListener, prox_sensor);
        unregisterReceiver(mBatInfoReceiver);
        System.out.println("Unbinding");
        return super.onUnbind(intent);
    }


    private CSVWriter generateWriterHeadings(CSVWriter writer, String csv, String[] entries) {
        if (!new File(csv).exists()) {    //write title to csv file
            try {
                Log.d("", "Created a new file");
                writer = new CSVWriter(new FileWriter(csv, true), ',');
                writer.writeNext(entries);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            writer = new CSVWriter(new FileWriter(csv, true), ',');
            return writer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeToCSV(String[] csvValues) {
    }

    private double getAmp(SoundMeter sm) {
        double amp;
        sm.start();
        amp = sm.getAmplitude();
        sm.stop();
        return amp;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            System.err.println("Permissions not granted");
            return ;
        }
        Log.d(TAG,"Connected Google API");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation!=null) {
            geoField = new GeomagneticField((float) mLocation.getLatitude(), (float) mLocation.getLongitude(), (float) mLocation.getAltitude(), System.currentTimeMillis());
            csv_2[2] = "" + geoField.getDeclination();
            System.out.println("Magnetic Declination is" + csv_2[2]);
        }
        else
        {
            Log.e(TAG,"Location is null");

        }
        return;
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.err.println("Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        System.err.println("Connection Failed");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            mSignalStrength = signalStrength.getGsmSignalStrength();
            mSignalStrength = (2 * mSignalStrength) - 113;
            csv_1[0]=""+mSignalStrength;// -> dBm
        }

    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public class binders extends Binder
    {
        MyService_Collection getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyService_Collection.this;
        }

    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public class SoundMeter {

        private AudioRecord ar = null;
        private int minSize;

        public void start() {
            minSize= AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,minSize);
            ar.startRecording();
        }

        public void stop() {
            if (ar != null) {
                ar.stop();
            }
        }

        public double getAmplitude() {
            short[] buffer = new short[minSize];
            ar.read(buffer, 0, minSize);
            int max = 0;
            for (short s : buffer)
            {
                if (Math.abs(s) > max)
                {
                    max = Math.abs(s);
                }
            }
            return max;
        }

    }

}

