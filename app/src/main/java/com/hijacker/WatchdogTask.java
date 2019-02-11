package com.hijacker;

/*
    Copyright (C) 2019  Christos Kyriakopoulos

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import static com.hijacker.MDKFragment.ados;
import static com.hijacker.MDKFragment.bf;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_MDK_BF;
import static com.hijacker.MainActivity.PROCESS_MDK_DOS;
import static com.hijacker.MainActivity.aireplay_running;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.error_notif;
import static com.hijacker.MainActivity.getPIDs;
import static com.hijacker.MainActivity.last_action;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.mNotificationManager;
import static com.hijacker.MainActivity.stop;

class WatchdogTask extends AsyncTask<Void, String, Boolean>{
    static final int SLEEP_TIME = 5000, PAUSE_TIME = 1000;
    Context con;
    WatchdogTask(Context context){
        this.con = context;
    }
    void sleep() throws InterruptedException{
        while(System.currentTimeMillis()-last_action < 1000){
            if(debug) Log.d("HIJACKER/watchdog", "Watchdog waiting for 1 sec...");
            Thread.sleep(PAUSE_TIME);
        }
    }
    void check(int process, boolean running, String stillRunning, String notRunning) throws InterruptedException{
        sleep();
        if(isCancelled()) throw new InterruptedException();

        List<Integer> list = getPIDs(process);
        if(running && list.size()==0){
            //process not running
            publishProgress(notRunning);
            stop(process);
        }else if(!running && list.size()>0){
            //process still running
            stop(process);      //Try to stop it
            if(getPIDs(process).size()>0){
                //Didn't work
                publishProgress(stillRunning);
            }
        }
    }
    @Override
    protected Boolean doInBackground(Void... params){
        try{
            while(!isCancelled()){
                if(debug) Log.d("HIJACKER/watchdog", "Watchdog watching...");

                check(PROCESS_AIRODUMP, Airodump.isRunning(), con.getString(R.string.airodump_still_running), con.getString(R.string.airodump_not_running));
                check(PROCESS_AIREPLAY, aireplay_running!=0, con.getString(R.string.aireplay_still_running), con.getString(R.string.aireplay_not_running));
                check(PROCESS_MDK_BF, bf, con.getString(R.string.mdk_still_running), con.getString(R.string.mdk_not_running));
                check(PROCESS_MDK_DOS, ados, con.getString(R.string.mdk_still_running), con.getString(R.string.mdk_not_running));
                //Can't check Reaver, it normally stops on its own, no way to know if there is a problem

                Thread.sleep(SLEEP_TIME);
            }

        }catch(InterruptedException e){
            Log.d("HIJACKER/watchdog", "Watchdog interrupted");
        }

        return true;
    }
    @Override
    protected void onProgressUpdate(String... progress){
        if(debug) Log.d("HIJACKER/watchdog", "Message is " + progress[0]);
        if(background){
            error_notif.setContentTitle(con.getString(R.string.watchdog_notif_title));
            error_notif.setContentText(progress[0]);
            mNotificationManager.notify(1, error_notif.build());
        }else{
            ErrorDialog dialog = new ErrorDialog();
            dialog.setTitle(progress[0]);
            dialog.setMessage(con.getString(R.string.watchdog_message));
            dialog.show(mFragmentManager, "ErrorDialog");
        }
    }
    boolean isRunning(){
        return getStatus()==Status.RUNNING;
    }
}
