package com.example.dellpc.iodetector_v10;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    RadioButton rb_indoor,rb_outdoor;
    Button b1,b2,b3,b_stop;
    TextView out;
    Intent it;
    MyService_Collection service;
    Boolean bounded;
    static String label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rb_indoor=(RadioButton)findViewById(R.id.rb_id);
        rb_outdoor=(RadioButton)findViewById(R.id.rb_od);
        b1=(Button)findViewById(R.id.button);
        b2=(Button)findViewById(R.id.button2);
        b3=(Button)findViewById(R.id.button3);
        b_stop=(Button)findViewById(R.id.button4);
        out=(TextView)findViewById(R.id.textView);
        service=new MyService_Collection();
        it=new Intent(this,MyService_Collection.class);

    }

    @Override
    protected void onStart() {
        super.onStart();
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rb_indoor.isChecked())
                {
                    label="Indoor";
                }
                else
                {
                    label="Outdoor";
                }
                bindService(it, mServiceConnection_collect,Context.BIND_AUTO_CREATE);

            }
        });
        b_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unbindService(mServiceConnection_collect);
            }
        });
    }


    private ServiceConnection mServiceConnection_collect = new ServiceConnection() {
        public static final String TAG = "Service Connection";
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bounded = false;
            Log.d(TAG, "Disconnected controller");
            unbindService(mServiceConnection_collect);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder myService) {
            MyService_Collection.binders myBinder = (MyService_Collection.binders) myService;
            service = myBinder.getService();
            bounded = true;
            Log.d(TAG,"onServiceConnected ");
            // startService(it);

        }
    };}
