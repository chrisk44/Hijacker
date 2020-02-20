package com.hijacker;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

class DialogRefreshTask extends AsyncTask<Void, Void, Boolean>{
    @SuppressLint("StaticFieldLeak")        // This object will exist as long as the device dialog exists
    DeviceDialog deviceDialog;
    DialogRefreshTask(@NonNull DeviceDialog deviceDialog){
        this.deviceDialog = deviceDialog;
    }
    @Override
    protected Boolean doInBackground(Void... params){
        try{
            while(deviceDialog.isResumed()){
                publishProgress();
                Thread.sleep(1000);
            }
        }catch(InterruptedException ignored){}
        return true;
    }
    @Override
    protected void onProgressUpdate(Void... progress){
        deviceDialog.onRefresh();
    }
}
