package com.hijacker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_MDK;
import static com.hijacker.MainActivity.stop;

public class StopReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        stop(PROCESS_AIREPLAY);
        stop(PROCESS_MDK);
    }
}
