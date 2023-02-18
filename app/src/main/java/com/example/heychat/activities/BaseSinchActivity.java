package com.example.heychat.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heychat.service.SinchService;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;

public abstract class BaseSinchActivity extends AppCompatActivity implements ServiceConnection {

    private SinchService.SinchServiceInterface mSinchServiceInterface;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = new Intent(this, SinchService.class);
        getApplicationContext().bindService(intent, this,
                BIND_AUTO_CREATE);
        Log.d("serviceapp", "BaseSinchActivity bindService");
//        preferenceManager = new PreferenceManager(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (SinchService.class.getName().equals(componentName.getClassName())) {
            mSinchServiceInterface = (SinchService.SinchServiceInterface) iBinder;
//            mSinchServiceInterface.startClient(preferenceManager.getString(Constants.KEY_USER_ID));
            onServiceConnected();
        }
        Log.d("serviceapp", "BaseSinchActivity onServiceConnected");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (SinchService.class.getName().equals(componentName.getClassName())) {
            mSinchServiceInterface.stopClient();
            mSinchServiceInterface = null;
            onServiceDisconnected();
            Log.d("stopClient", "BaseSinchActivity onServiceDisconnected");
        }
    }

    protected void onServiceConnected() {

    }

    protected void onServiceDisconnected() {
        onStop();
    }

    protected SinchService.SinchServiceInterface getSinchServiceInterface() {
        return mSinchServiceInterface;
    }

}
