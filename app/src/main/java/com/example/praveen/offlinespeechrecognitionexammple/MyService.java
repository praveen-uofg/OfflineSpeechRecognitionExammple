package com.example.praveen.offlinespeechrecognitionexammple;


import static android.util.Log.i;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.ArrayAdapter;

public class MyService extends Service
{
    protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer = null;;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private boolean mIsStreamSolo = false;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;
    
    private Handler mHandler = new Handler();


    @Override
    public void onCreate()
    {
        Log.d("MyService","onCreate");
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);       
        setUpSpeechRecognizer();

    }
    
    public void setUpSpeechRecognizer() {
    	i("MyService","setUpSpeechRecognizer");
    	if (mSpeechRecognizer == null) {
    	i("MyService","setUpSpeechRecognizer mSpeechRecognizer is null");
    	mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
    	}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyService","onStartCommand");
        /*try
        {
            Message msg = new Message();
            msg.what = MSG_RECOGNIZER_START_LISTENING;
            mServerMessenger.send(msg);
        }
        catch (RemoteException e)
        {

        }*/
        return  START_NOT_STICKY;
    }

    protected static class IncomingHandler extends Handler
    {
        private WeakReference<MyService> mtarget;

        IncomingHandler(MyService target)
        {
            mtarget = new WeakReference<MyService>(target);
        }


        @Override
        public void handleMessage(Message msg)
        {
            final MyService target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        // turn off beep sound
                        if (!target.mIsStreamSolo)
                        {
                            target.mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                            target.mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening)
                    {
                    	target.setUpSpeechRecognizer();
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        Log.d("MyService", "message start listening"); //$NON-NLS-1$
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (target.mIsStreamSolo)
                    {
                        target.mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                        target.mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    target.destroySpeechRecognizerInstance();
                    Log.d("MyService", "message canceled recognizer"); //$NON-NLS-1$
                    break;
            }
        }
    }
    
    public void destroySpeechRecognizerInstance() {
    	i("MyService"," destroySpeechRecognizerInstance");
    	if (mSpeechRecognizer != null)
        {
    		i("MyService"," destroySpeechRecognizerInstance mSpeechRecognizer is not null");
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
    }

    
    Runnable mNoSpeechCountDown = new Runnable() {
		
		@Override
		public void run() {
			Log.d("MyService"," Runnable mNoSpeechCountDown");
			mHandler.removeCallbacks(mNoSpeechCountDown);
            mIsCountDownOn = false;
            if (!mIsListening) {            
            try
            {
            	Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
                mServerMessenger.send(message);
                
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
               
            }
            catch (RemoteException e)
            {

            }
			
		  }
		}
	};
    

    @Override
    public void onDestroy()
    {
        Log.d("MyService","onDestroy");
        super.onDestroy();

        if (mIsCountDownOn)
        {
            mHandler.removeCallbacks(mNoSpeechCountDown);
        }
        
        destroySpeechRecognizerInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServerMessenger.getBinder();
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	 i("MyService", "onUnbind()");
		return false;
    }

    protected class SpeechRecognitionListener implements RecognitionListener
    {

        @Override
        public void onBeginningOfSpeech()
        {
            // speech input will be processed, so there is no need for count down anymore
        	mIsCountDownOn = false;
        	mHandler.removeCallbacks(mNoSpeechCountDown);
            mIsListening = true;
            Log.d("MyService", "onBeginingOfSpeech"); 
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {
            Log.d("MyService", "onBufferReceived");

        }

        @Override
        public void onEndOfSpeech()
        {
        	mIsListening = false;
            Log.d("MyService", "onEndOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onError(int error)
        {
            if ((error == SpeechRecognizer.ERROR_NO_MATCH)|| (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
            {
                Log.d("MyService", "didn't recognize anything");
                try
                {
                	mIsListening = false;
                    Message msg = Message.obtain(null,MSG_RECOGNIZER_START_LISTENING);
                    mServerMessenger.send(msg);
                }
                catch (RemoteException e)
                {

                }
                
                
            }
            else 
            {
                Log.d("MyService"," Error code = " +error);
                
                if (mIsCountDownOn)
                {
                    mHandler.removeCallbacks(mNoSpeechCountDown);
                }
                try
                {
                	Message msg = Message.obtain(null,MSG_RECOGNIZER_CANCEL);
                	mServerMessenger.send(msg);

                    msg = Message.obtain(null,MSG_RECOGNIZER_START_LISTENING);
                    mServerMessenger.send(msg);
                }
                catch (RemoteException e)
                {

                }
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {
            Log.d("MyService","onEvent ( " + eventType + "params = " + params +")" );
        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {
            Log.d("MyService","onPartialResults");
        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                mIsCountDownOn = true;
                
                mIsListening = false;
                mHandler.postDelayed(mNoSpeechCountDown, 5000);

            }
            Log.d("MyService", "onReadyForSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onResults(Bundle results)
        {
            Log.d("MyService", "onResults result = " + results);

            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            MainActivity.wordsList.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, matches));
            for (int i =0;i<matches.size();i++) {
                Log.d("MyService","result = " + matches.get(i));
            }
            if (mIsCountDownOn)
            {
               mIsCountDownOn = false;
               mHandler.removeCallbacks(mNoSpeechCountDown);
            }
            mIsListening = false;
            Log.d("MyService", "onResults()");

            try
            {
                Message msg = Message.obtain(null,MSG_RECOGNIZER_START_LISTENING);

                mServerMessenger.send(msg);
            }
            catch (RemoteException e)
            {

            }
        }

        @Override
        public void onRmsChanged(float rmsdB)
        {
            Log.d("MyService","onRmsChanged");
        }

    }
}