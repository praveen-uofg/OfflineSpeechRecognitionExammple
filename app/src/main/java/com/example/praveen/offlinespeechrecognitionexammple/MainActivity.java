package com.example.praveen.offlinespeechrecognitionexammple;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ListView;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
   public static ListView wordsList;
    private int mBindFlag;
    private Messenger mServiceMessenger;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wordsList = (ListView)findViewById(R.id.list);
        Intent service = new Intent(this, MyService.class);
        startService(service);
        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, MyService.class), mServiceConnection, mBindFlag);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mServiceConnection!=null) {
            unbindService(mServiceConnection);
            mServiceMessenger = null;

        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {

                Log.d("MainActivity", "onServiceConnected");

            mServiceMessenger = new Messenger(service);
            Message msg = new Message();
            msg.what = MyService.MSG_RECOGNIZER_START_LISTENING;

            try
            {
                mServiceMessenger.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d("MainActivity", "onServiceDisconnected");
            mServiceMessenger = null;
        }

    }; // mServiceConnection


}