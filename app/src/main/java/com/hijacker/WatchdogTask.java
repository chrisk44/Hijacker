package com.hijacker;

/*
    Copyright (C) 2017  Christos Kyriakopoylos

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
import static com.hijacker.MainActivity.PROCESS_MDK;
import static com.hijacker.MainActivity.PROCESS_REAVER;
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
    @Override
    protected Boolean doInBackground(Void... params){
        try{
            while(!isCancelled()){
                Thread.sleep(SLEEP_TIME);
                if(isCancelled()) return false;
                while(System.currentTimeMillis()-last_action < 1000){
                    if(debug) Log.d("HIJACKER/watchdog", "Watchdog waiting for 1 sec...");
                    Thread.sleep(PAUSE_TIME);
                    if(isCancelled()) return false;
                }
                if(debug) Log.d("HIJACKER/watchdog", "Watchdog watching...");
                List<Integer> list;
                if(isCancelled()) return false;
                list = getPIDs(PROCESS_AIRODUMP);
                if(Airodump.isRunning() && list.size()==0){          //airodump not running
                    publishProgress(con.getString(R.string.airodump_not_running));
                    stop(PROCESS_AIRODUMP);
                }else if(!Airodump.isRunning() && list.size()>0){     //airodump still running
                    if(debug) Log.d("HIJACKER/watchdog", "Airodump is still running. Trying to kill it...");
                    stop(PROCESS_AIRODUMP);
                    if(getPIDs(PROCESS_AIRODUMP).size()>0){
                        publishProgress(con.getString(R.string.airodump_still_running));
                    }
                }
                if(isCancelled()) return false;
                list = getPIDs(PROCESS_AIREPLAY);
                if(aireplay_running!=0 && list.size()==0){      //aireplay not running
                    publishProgress(con.getString(R.string.aireplay_not_running));
                    stop(PROCESS_AIREPLAY);
                }else if(aireplay_running==0 && list.size()>0){ //aireplay still running
                    if(debug) Log.d("HIJACKER/watchdog", "Aireplay is still running. Trying to kill it...");
                    stop(PROCESS_AIREPLAY);
                    if(getPIDs(PROCESS_AIREPLAY).size()>0){
                        publishProgress(con.getString(R.string.aireplay_still_running));
                    }
                }
                if(isCancelled()) return false;
                list = getPIDs(PROCESS_MDK);
                if((bf || ados) && list.size()==0){         //mdk not running
                    publishProgress(con.getString(R.string.mdk_not_running));
                    stop(PROCESS_MDK);
                }else if(!(bf || ados) && list.size()>0){   //mdk still running
                    if(debug) Log.d("HIJACKER/watchdog", "MDK is still running. Trying to kill it...");
                    stop(PROCESS_MDK);
                    if(getPIDs(PROCESS_MDK).size()>0){
                        publishProgress(con.getString(R.string.mdk_still_running));
                    }
                }
                if(isCancelled()) return false;
                list = getPIDs(PROCESS_REAVER);
                if(ReaverFragment.isRunning() && list.size()==0){         //reaver not running
                    publishProgress(con.getString(R.string.reaver_not_running));
                    stop(PROCESS_REAVER);
                }else if(!ReaverFragment.isRunning() && list.size()>0){   //reaver still running
                    if(debug) Log.d("HIJACKER/watchdog", "Reaver is still running. Trying to kill it...");
                    stop(PROCESS_REAVER);
                    if(getPIDs(PROCESS_REAVER).size()>0){
                        publishProgress(con.getString(R.string.reaver_still_running));
                    }
                }
            }
        }catch(InterruptedException e){
            Log.e("HIJACKER/watchdog", "Exception: " + e.toString());
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
