package com.hijacker;

/*
    Copyright (C) 2016  Christos Kyriakopoylos

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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import static com.hijacker.MainActivity.nm;
import static com.hijacker.MainActivity.notif2;
import static com.hijacker.MainActivity.notif_on;
import static com.hijacker.MainActivity.watchdog_runnable;
import static com.hijacker.MainActivity.watchdog_thread;

public class ErrorDialog extends DialogFragment {
    static String notification2_title;
    String message;
    String title=null;
    boolean watchdog=false;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(title==null) title = getString(R.string.error);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(!watchdog_thread.isAlive() && watchdog){
                    watchdog_thread = new Thread(watchdog_runnable);
                    watchdog_thread.start();   //If the error was from there restart the thread
                }
            }
        });
        builder.setNeutralButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getActivity().finish();
            }
        });
        if(message!=null) {
            builder.setTitle(title);
            builder.setMessage(this.message);
        }else{
            Log.d("HIJACKER/ErrorDialog", "Message not set");
            builder.setTitle(R.string.eoe_title);
            builder.setMessage(R.string.eoe_message);
        }
        return builder.create();
    }
    public void setMessage(String msg){ this.message = msg; }
    public void setTitle(String title){ this.title = title; }
    public void setWatchdog(boolean wd){
        this.watchdog = wd;
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!notif_on) super.show(fragmentManager, tag);
        else{
            if(this.watchdog){
                notif2.setContentTitle(notification2_title);
                notif2.setContentText(title);
            }else{
                notif2.setContentTitle(title);
                notif2.setContentText(message);
            }
            nm.notify(1, notif2.build());
        }
    }
}
