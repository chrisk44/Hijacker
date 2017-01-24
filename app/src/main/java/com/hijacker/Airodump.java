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

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.adapter;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.airodump_running;
import static com.hijacker.MainActivity.always_cap;
import static com.hijacker.MainActivity.ap_count;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.completed;
import static com.hijacker.MainActivity.cont;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.enable_monMode;
import static com.hijacker.MainActivity.fifo;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.last_action;
import static com.hijacker.MainActivity.main;
import static com.hijacker.MainActivity.notification;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.refreshState;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.st_count;
import static com.hijacker.MainActivity.toSort;
import static com.hijacker.MainActivity.menu;
import static com.hijacker.Shell.runOne;

public class Airodump{
    static int channel = 0;
    static boolean forWPA = false, forWEP = false, running = false;
    static String mac = null;
    static Thread refresh_thread;
    static Runnable refresh_runnable = new Runnable(){
        @Override
        public void run(){
            if(debug) Log.d("HIJACKER/refresh_thread", "refresh_thread running");
            try{
                while(cont){
                    while(!fifo.isEmpty()){
                        synchronized(fifo){
                            fifo.pop().add();
                        }
                        while(!completed){      //Wait for the update request to complete
                            Thread.sleep(10);
                        }
                    }
                    runInHandler(new Runnable(){
                        @Override
                        public void run(){
                            adapter.notifyDataSetChanged();             //for when data is changed, no new data added
                            ap_count.setText(Integer.toString(is_ap==null ? Tile.i : 1));
                            st_count.setText(Integer.toString(Tile.tiles.size() - Tile.i));
                            notification();
                            if(toSort && !background) Tile.sort();
                        }
                    });
                    Thread.sleep(1000);
                }
            }catch(InterruptedException e){
                Log.e("HIJACKER/Exception", "Caught Exception in main() refresh_thread block: " + e.toString());
                fifo.clear();
                try{
                    while(!completed){      //Wait for running request to complete
                        Thread.sleep(10);
                    }
                }catch(InterruptedException ignored){}
            }
            if(debug) Log.d("HIJACKER/refresh_thread", "refresh_thread done");
        }
    };
    static void reset(){
        if(isRunning()){
            Log.e("HIJACKER/Airodump", "Can't reset while airodump is running");
            return;
        }
        channel = 0;
        forWPA = false;
        forWEP = false;
        mac = null;
    }
    static void setChannel(int ch){
        if(isRunning()){
            Log.e("HIJACKER/Airodump", "Can't change settings while airodump is running");
            return;
        }
        channel = ch;
    }
    static void setMac(String new_mac){
        if(isRunning()){
            Log.e("HIJACKER/Airodump", "Can't change settings while airodump is running");
            return;
        }
        mac = new_mac;
    }
    static void setForWPA(boolean bool){
        if(isRunning()){
            Log.e("HIJACKER/Airodump", "Can't change settings while airodump is running");
            return;
        }
        if(forWEP){
            Log.e("HIJACKER/Airodump", "Can't set forWPA when forWEP is enabled");
            return;
        }
        forWPA = bool;
    }
    static void setForWEP(boolean bool){
        if(isRunning()){
            Log.e("HIJACKER/Airodump", "Can't change setting while airodump is running");
            return;
        }
        if(forWPA){
            Log.e("HIJACKER/Airodump", "Can't set forWEP when forWPA is enabled");
            return;
        }
        forWEP = bool;
    }
    static void setAP(AP ap){
        if(isRunning()){
            Log.e("HIJACKER/Airodump", "Can't change setting while airodump is running");
            return;
        }
        mac = ap.mac;
        channel = ap.ch;
    }
    static void start(){
        String cmd = "su -c " + prefix + " " + airodump_dir + " --update 1 ";

        if(forWPA) cmd += "-w " + cap_dir + "/handshake ";
        else if(forWEP) cmd += "-w " + cap_dir + "/wep_ivs ";
        else if(always_cap) cmd += "-w " + cap_dir + "/cap ";

        if(channel!=0) cmd += "--channel " + channel + " ";

        if(mac!=null) cmd += "--bssid " + mac + " ";

        cmd += iface;

        runOne(enable_monMode);
        stop();

        final String final_cmd = cmd;
        cont = true;
        new Thread(new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("HIJACKER/startAirodump", final_cmd);
                try{
                    int mode = forWEP || forWPA ? 1 : 0;
                    Process process = Runtime.getRuntime().exec(final_cmd);
                    running = true;
                    last_action = System.currentTimeMillis();
                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String buffer;
                    while(cont && (buffer = in.readLine())!=null){
                        main(buffer, mode);
                    }
                }catch(IOException e){ Log.e("HIJACKER/Exception", "Caught Exception in startAirodump() read block: " + e.toString()); }
            }
        }).start();
        refresh_thread = new Thread(refresh_runnable);
        refresh_thread.start();
        airodump_running = 1;
        runInHandler(new Runnable(){
            @Override
            public void run(){
                if(menu!=null) menu.getItem(1).setIcon(R.drawable.stop);
                refreshState();
                notification();
            }
        });
    }
    static void stop(){
        MainActivity.stop(PROCESS_AIRODUMP);
        reset();
    }
    static boolean isRunning(){
        return running;
    }
}
