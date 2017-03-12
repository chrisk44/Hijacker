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
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

import static com.hijacker.MainActivity.CHROOT_BIN_MISSING;
import static com.hijacker.MainActivity.CHROOT_DIR_MISSING;
import static com.hijacker.MainActivity.CHROOT_FOUND;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_MDK;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.checkChroot;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.notif_on;
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
    View dialogView;
    TextView test_cur_cmd;
    ProgressBar test_progress;
    Thread thread;
    Runnable runnable = new Runnable(){
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
                        Log.d("HIJACKER/test_thread", cmd);
                        runOne(cmd);
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException ignored){}
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
                        Log.d("HIJACKER/test_thread", cmd);
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
                        String cmd = "su -c " + prefix + " " + aireplay_dir + " -D --deauth 0 -a 11:22:33:44:55:66 " + iface;
                        Log.d("HIJACKER/test_thread", cmd);
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
                        Log.d("HIJACKER/test_thread", cmd);
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
                        Log.d("HIJACKER/test_thread", cmd);
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
                        int chroot_check = checkChroot();
                        if(chroot_check!=CHROOT_FOUND){
                            status[4].setImageResource(R.drawable.failed);
                            if(chroot_check==CHROOT_DIR_MISSING) test_cur_cmd.setText(R.string.chroot_notfound);
                            else if(chroot_check==CHROOT_BIN_MISSING) test_cur_cmd.setText(R.string.kali_notfound);
                            else test_cur_cmd.setText(R.string.chroot_both_notfound);
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
                Log.d("HIJACKER/test_thread", "Interrupted");
            }finally{
                if(watchdog){
                    watchdog_thread = new Thread(watchdog_runnable);
                    watchdog_thread.start();
                }
            }
        }
    };
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        load();
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.test, null);

        test_progress = (ProgressBar)dialogView.findViewById(R.id.test_progress);
        status[0] = (ImageView)dialogView.findViewById(R.id.imageView1);
        status[1] = (ImageView)dialogView.findViewById(R.id.imageView2);
        status[2] = (ImageView)dialogView.findViewById(R.id.imageView3);
        status[3] = (ImageView)dialogView.findViewById(R.id.imageView4);
        status[4] = (ImageView)dialogView.findViewById(R.id.imageView5);
        test_cur_cmd = (TextView)dialogView.findViewById(R.id.current_cmd);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        test_progress.setProgress(0);
        status[0].setImageResource(android.R.color.transparent);
        status[1].setImageResource(android.R.color.transparent);
        status[2].setImageResource(android.R.color.transparent);
        status[3].setImageResource(android.R.color.transparent);
        status[4].setImageResource(android.R.color.transparent);
        test_cur_cmd.setText(enable_monMode);

        thread = new Thread(runnable);
        thread.start();

        builder.setView(dialogView);
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
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!notif_on) super.show(fragmentManager, tag);
    }
}
