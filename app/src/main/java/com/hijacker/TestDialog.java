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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_MDK;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.mdk3_dir;
import static com.hijacker.MainActivity.reaver_dir;
import static com.hijacker.MainActivity.enable_monMode;
import static com.hijacker.MainActivity.getPIDs;
import static com.hijacker.MainActivity.load;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.status;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.MainActivity.watchdog;
import static com.hijacker.MainActivity.watchdog_runnable;
import static com.hijacker.MainActivity.watchdog_thread;
import static com.hijacker.Shell.runOne;

public class TestDialog extends DialogFragment {
    boolean test_wait;
    TextView test_cur_cmd;
    ProgressBar test_progress;
    Thread thread;
    Runnable runnable;
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        load();

        runnable = new Runnable(){
            @Override
            public void run(){
                watchdog_thread.interrupt();
                try{
                    Thread.sleep(500);
                    //Separate calls so the UI can be refreshed, otherwise it gets blocked.
                    test_wait = true;
                    runInHandler(new Runnable(){        //stop everything and turn on monitor mode
                        @Override
                        public void run(){
                            stop(PROCESS_AIRODUMP);
                            stop(PROCESS_AIREPLAY);
                            stop(PROCESS_MDK);
                            String cmd = enable_monMode;
                            Log.d("test_thread", cmd);
                            try{
                                Runtime.getRuntime().exec(cmd);
                                Thread.sleep(1000);
                            }catch(IOException | InterruptedException ignored){}
                            status[0].setImageResource(R.drawable.testing);
                            test_cur_cmd.setText("su -c " + prefix + " " + airodump_dir + " " + iface);
                            test_wait = false;
                        }
                    });
                    while(test_wait){
                        Thread.sleep(100);
                    }

                    test_wait = true;
                    runInHandler(new Runnable(){            //test airodump
                        @Override
                        public void run(){
                            String cmd = "su -c " + prefix + " " + airodump_dir + " " + iface;
                            Log.d("test_thread", cmd);
                            try{
                                Runtime.getRuntime().exec(cmd);
                                Thread.sleep(1000);
                            }catch(IOException | InterruptedException ignored){}
                            if(getPIDs(PROCESS_AIRODUMP).size()==0) status[0].setImageResource(R.drawable.failed);
                            else{
                                stop(PROCESS_AIRODUMP);
                                status[0].setImageResource(R.drawable.passed);
                            }
                            test_progress.setProgress(1);
                            status[1].setImageResource(R.drawable.testing);
                            test_cur_cmd.setText("su -c " + prefix + " " + aireplay_dir + " --deauth 0 -a 11:22:33:44:55:66 " + iface);
                            test_wait = false;
                        }
                    });
                    while(test_wait){
                        Thread.sleep(100);
                    }

                    test_wait = true;
                    runInHandler(new Runnable(){        //test aireplay
                        @Override
                        public void run(){
                            String cmd = "su -c " + prefix + " " + aireplay_dir + " --deauth 0 -a 11:22:33:44:55:66 " + iface;
                            Log.d("test_thread", cmd);
                            try{
                                Runtime.getRuntime().exec(cmd);
                                Thread.sleep(1000);
                            }catch(IOException | InterruptedException ignored){}
                            if(getPIDs(PROCESS_AIREPLAY).size()==0) status[1].setImageResource(R.drawable.failed);
                            else{
                                stop(PROCESS_AIREPLAY);
                                status[1].setImageResource(R.drawable.passed);
                            }
                            test_progress.setProgress(2);
                            status[2].setImageResource(R.drawable.testing);
                            test_cur_cmd.setText("su -c " + prefix + " " + mdk3_dir + " " + iface + " b -m");
                            test_wait = false;
                        }
                    });
                    while(test_wait){
                        Thread.sleep(100);
                    }

                    test_wait = true;
                    runInHandler(new Runnable(){            //test mdk
                        @Override
                        public void run(){
                            String cmd = "su -c " + prefix + " " + mdk3_dir + " " + iface + " b -m";
                            Log.d("test_thread", cmd);
                            try{
                                Runtime.getRuntime().exec(cmd);
                                Thread.sleep(1000);
                            }catch(IOException | InterruptedException ignored){}
                            if(getPIDs(PROCESS_MDK).size()==0) status[2].setImageResource(R.drawable.failed);
                            else{
                                stop(PROCESS_MDK);
                                status[2].setImageResource(R.drawable.passed);
                            }
                            test_progress.setProgress(3);
                            status[3].setImageResource(R.drawable.testing);
                            test_cur_cmd.setText("su -c " + prefix + " " + reaver_dir + " -i " + iface + " -b 00:11:22:33:44:55 -c 2");
                            test_wait = false;
                        }
                    });
                    while(test_wait){
                        Thread.sleep(100);
                    }

                    test_wait = true;
                    runInHandler(new Runnable(){            //test reaver
                        @Override
                        public void run(){
                            String cmd = "su -c " + prefix + " " + reaver_dir + " -i " + iface + " -b 00:11:22:33:44:55 -c 2";
                            Log.d("test_thread", cmd);
                            try{
                                Runtime.getRuntime().exec(cmd);
                                Thread.sleep(1000);
                            }catch(IOException | InterruptedException ignored){}
                            if(getPIDs(PROCESS_REAVER).size()==0) status[3].setImageResource(R.drawable.failed);
                            else{
                                stop(PROCESS_REAVER);
                                status[3].setImageResource(R.drawable.passed);
                            }
                            test_progress.setProgress(4);
                            status[4].setImageResource(R.drawable.testing);
                            test_cur_cmd.setText(R.string.checking_chroot);
                            test_wait = false;
                        }
                    });
                    while(test_wait){
                        Thread.sleep(100);
                    }

                    test_wait = true;
                    runInHandler(new Runnable(){        //check chroot
                        @Override
                        public void run(){
                            runOne("chmod a+r " + MainActivity.chroot_dir);
                            File chroot_dir = new File(MainActivity.chroot_dir);
                            boolean kali_init = false;
                            try{
                                Process dc = Runtime.getRuntime().exec("ls /system/bin -1 | grep bootkali_init");
                                BufferedReader out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                                kali_init = out.readLine()!=null;
                            }catch(IOException ignored){}
                            Log.d("test_thread", "chroot_dir is " + Boolean.toString(chroot_dir.exists()));
                            Log.d("test_thread", "kali_init is " + Boolean.toString(kali_init));
                            if(!chroot_dir.exists() || !kali_init){
                                status[4].setImageResource(R.drawable.failed);
                                if(!chroot_dir.exists()) test_cur_cmd.setText(R.string.chroot_notfound);
                                else if(!kali_init) test_cur_cmd.setText(R.string.kali_notfound);
                            }else{
                                test_cur_cmd.setText(R.string.done);
                                status[4].setImageResource(R.drawable.passed);
                            }
                            test_progress.setProgress(5);
                            test_wait = false;

                            stop(PROCESS_AIRODUMP);
                            stop(PROCESS_AIREPLAY);
                            stop(PROCESS_MDK);
                            stop(PROCESS_REAVER);
                            test_progress.setProgress(6);
                        }
                    });
                    while(test_wait){
                        Thread.sleep(100);
                    }
                }catch(InterruptedException e){
                    Log.d("test_thread", "Interrupted");
                }finally{
                    if(watchdog){
                        watchdog_thread = new Thread(watchdog_runnable);
                        watchdog_thread.start();
                    }
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.test, null);

        test_progress = (ProgressBar)view.findViewById(R.id.test_progress);
        test_progress.setProgress(0);
        status[0] = (ImageView)view.findViewById(R.id.imageView1);
        status[1] = (ImageView)view.findViewById(R.id.imageView2);
        status[2] = (ImageView)view.findViewById(R.id.imageView3);
        status[3] = (ImageView)view.findViewById(R.id.imageView4);
        status[4] = (ImageView)view.findViewById(R.id.imageView5);
        status[0].setImageResource(android.R.color.transparent);
        status[1].setImageResource(android.R.color.transparent);
        status[2].setImageResource(android.R.color.transparent);
        status[3].setImageResource(android.R.color.transparent);
        status[4].setImageResource(android.R.color.transparent);
        test_cur_cmd = (TextView)view.findViewById(R.id.current_cmd);
        test_cur_cmd.setText(enable_monMode);

        thread = new Thread(runnable);
        thread.start();

        builder.setView(view);
        builder.setTitle(R.string.testing);
        builder.setMessage(R.string.make_sure_wifi);
        builder.setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
                thread.interrupt();
            }
        });
        return builder.create();
    }
}
