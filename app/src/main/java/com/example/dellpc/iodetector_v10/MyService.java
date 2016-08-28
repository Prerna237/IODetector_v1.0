package com.example.dellpc.iodetector_v10;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MyService extends Service {
    public final String TAG="MyService";
    private IBinder binder_service;
    String[] headers_c1,headers_c2;
    Thread t_css_1,t_li_1,t_pv_1,t_bt_2,t_sa_2,t_mv_2;
    String[] csv_1=new String[4];
    String[] csv_2=new String[3];
    String csv_file_name_1;
    String csv_file_name_2;
    CSVWriter writer1= null;
    CSVWriter writer2 = null;
    GeomagneticField geoField;
    TelephonyManager mTelephonyManager;
    MyPhoneStateListener mPhoneStatelistener;
    int mSignalStrength = 0;
    Boolean flag,flag0,flag1,flag2,flag3;
    SensorManager sensorManager;
    Sensor light_sensor;
    SensorEventListener lightEventListener;
    Sensor prox_sensor;
    SensorEventListener proxEventListener;
    Sensor mag_sensor;
    SensorEventListener magEventListener;
    float light_intensity_val;
    BroadcastReceiver mBatInfoReceiver;
    SoundMeter sm;
    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //cell signal strength, light intensity, time of day and proximity value
        //battery temperature, sound amplitude  magnetic variance
        sm=new SoundMeter();
        csv_file_name_1 = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+"/class_1"+".csv";
        csv_file_name_2 = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+"/class_2"+".csv";
        headers_c1= new String[]{"Cell_Signal_Strength","Light_Intensity","Time","Proximity_val"};
        headers_c2=new String[]{"Battery temp","sound_amp","magnetic_variance"};
        mPhoneStatelistener = new MyPhoneStateListener();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        light_sensor= sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        prox_sensor=sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mag_sensor=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Log.d(TAG, "Light Sensor:" + light_sensor);
        Log.d(TAG, "Prox Sensor:" + light_sensor);

    }



    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Inside onBind of service");
        flag=false;flag0=false;flag1=false;flag2=false;flag3=false;
        writer1=generateWriterHeadings(writer1,csv_file_name_1,headers_c1);
        writer2=generateWriterHeadings(writer2,csv_file_name_2,headers_c2);
        sensorManager.registerListener(lightEventListener, light_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(proxEventListener, prox_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        t_css_1=new Thread(new Runnable() {
            @Override
            public void run() {
                mPhoneStatelistener = new MyPhoneStateListener();
                mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                mTelephonyManager.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                System.out.println("Signal Strength"+mSignalStrength);
                csv_1[0]=Integer.toString(mSignalStrength);
                flag0=true;
            }
        });
        t_li_1=new Thread(new Runnable() {
            @Override
            public void run() {
                while(lightEventListener==null)
                {
                    lightEventListener=new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            light_intensity_val=event.values[0];
                            csv_1[1]=Float.toString(light_intensity_val);
                            System.out.println("light:"+event.values[0]);
                            flag1=true;
                            sensorManager.unregisterListener(lightEventListener,light_sensor);
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {

                        }
                    };
                }
                sensorManager.registerListener(lightEventListener,light_sensor,SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        t_pv_1=new Thread(new Runnable() {
            @Override
            public void run() {
                while(proxEventListener==null)
                {
                    proxEventListener=new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            csv_1[3]=Float.toString(event.values[0]);
                            System.out.println("prox:"+event.values[0]);
                            sensorManager.unregisterListener(proxEventListener,prox_sensor);
                            flag2=true;
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {

                        }
                    };
                }
                sensorManager.registerListener(proxEventListener,prox_sensor,SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        mBatInfoReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent intent) {
                // TODO Auto-generated method stub
                int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10;
                System.out.println("battery"+temp);
                csv_2[0]=Integer.toString(temp);
                flag3=true;
            }
        };
        csv_2[1]=Double.toString(getAmp(sm)/10);
        this.registerReceiver(this.mBatInfoReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        csv_1[2]=""+(System.currentTimeMillis());
        //csv_2[1]="audio";
        csv_2[2]="location";
        while(flag==false) {
            if (flag1 == true && flag2 == true && flag3 == true && flag0 == true) {
                flag = true;
            }
        }
        return binder_service;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        while(true)
        {
            System.out.println("Entered loop");
            if (flag==true) {
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
            else
            {
                ;
            }
        }
    }

    private CSVWriter generateWriterHeadings(CSVWriter writer,String csv,String[] entries)
    {
        if(!new File(csv).exists())
        {    //write title to csv file
            try {
                Log.d("","Created a new file");
                writer = new CSVWriter(new FileWriter(csv, true), ',');
                writer.writeNext(entries);
            }
            catch (IOException e) {
                e.printStackTrace();}

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
    private void writeToCSV(String[] csvValues)
    {}

    private double getAmp(SoundMeter sm)
    {
        double amp;
        sm.start();
        amp=sm.getAmplitude();
        sm.stop();
        return amp;
    }

    class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            mSignalStrength = signalStrength.getGsmSignalStrength();
            mSignalStrength = (2 * mSignalStrength) - 113;// -> dBm
        }

    }

    public class binders extends Binder
    {
        MyService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyService.this;
        }

    }
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

