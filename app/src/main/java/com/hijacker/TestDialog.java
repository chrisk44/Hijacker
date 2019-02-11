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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

import static com.hijacker.MainActivity.CHROOT_BIN_MISSING;
import static com.hijacker.MainActivity.CHROOT_DIR_MISSING;
import static com.hijacker.MainActivity.CHROOT_FOUND;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_MDK_BF;
import static com.hijacker.MainActivity.PROCESS_MDK_DOS;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.checkChroot;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.last_action;
import static com.hijacker.MainActivity.mdk3bf_dir;
import static com.hijacker.MainActivity.notif_on;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.reaver_dir;
import static com.hijacker.MainActivity.enable_monMode;
import static com.hijacker.MainActivity.getPIDs;
import static com.hijacker.MainActivity.load;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.status;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.Shell.runOne;

public class TestDialog extends DialogFragment {
    static final int TEST_WAIT = 500;
    View dialogView;
    TextView test_cur_cmd;
    ProgressBar test_progress;
    Thread thread;
    final Runnable runnable = new Runnable(){
        @Override
        public void run(){
            final boolean results[] = {false, false, false, false, false};
            final String cmdMonMode = enable_monMode;
            final String cmdAirodump = "su -c " + prefix + " " + airodump_dir + " " + iface;
            final String cmdAireplay = "su -c " + prefix + " " + aireplay_dir + " --deauth 0 -a 11:22:33:44:55:66 " + iface;
            final String cmdMdk = "su -c " + prefix + " " + mdk3bf_dir + " " + iface + " b -m";
            final String cmdReaver = "su -c " + prefix + " " + reaver_dir + " -i " + iface + " -b 00:11:22:33:44:55 -c 2";
            try{
                stop(PROCESS_AIRODUMP);
                stop(PROCESS_AIREPLAY);
                stop(PROCESS_MDK_BF);
                stop(PROCESS_MDK_DOS);
                stop(PROCESS_REAVER);
                last_action = System.currentTimeMillis() + 10000;       //Make watchdog wait until the test is over

                //Enable monitor mode
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        test_cur_cmd.setText(enable_monMode);
                    }
                });
                Log.d("HIJACKER/test_thread", cmdMonMode);
                runOne(cmdMonMode);
                Thread.sleep(500);
                runInHandler(new Runnable(){        //stop everything and turn on monitor mode
                    @Override
                    public void run(){
                        status[0].setImageResource(R.drawable.testing_drawable);
                        test_cur_cmd.setText(cmdAirodump);
                    }
                });

                //Airodump
                Log.d("HIJACKER/test_thread", cmdAirodump);
                Runtime.getRuntime().exec(cmdAirodump);
                Thread.sleep(TEST_WAIT);

                if(getPIDs(PROCESS_AIRODUMP).size()==0) thread.interrupt();
                else{
                    stop(PROCESS_AIRODUMP);
                    last_action = System.currentTimeMillis() + 10000;
                    results[0] = true;
                }
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        status[0].setImageResource(results[0] ? R.drawable.done_drawable : R.drawable.failed_drawable);
                        test_progress.setProgress(1);

                        test_cur_cmd.setText(cmdAireplay);
                        status[1].setImageResource(R.drawable.testing_drawable);
                    }
                });

                //Aireplay
                Log.d("HIJACKER/test_thread", cmdAireplay);
                Runtime.getRuntime().exec(cmdAireplay);
                Thread.sleep(TEST_WAIT);

                if(getPIDs(PROCESS_AIREPLAY).size()==0) results[1] = false;
                else{
                    stop(PROCESS_AIREPLAY);
                    last_action = System.currentTimeMillis() + 10000;
                    results[1] = true;
                }
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        status[1].setImageResource(results[1] ? R.drawable.done_drawable : R.drawable.failed_drawable);
                        test_progress.setProgress(2);

                        status[2].setImageResource(R.drawable.testing_drawable);
                        test_cur_cmd.setText(cmdMdk);
                    }
                });

                //MDK
                Log.d("HIJACKER/test_thread", cmdMdk);
                Runtime.getRuntime().exec(cmdMdk);
                Thread.sleep(TEST_WAIT);

                if(getPIDs(PROCESS_MDK_BF).size()==0) results[2] = false;
                else{
                    stop(PROCESS_MDK_BF);
                    last_action = System.currentTimeMillis() + 10000;
                    results[2] = true;
                }
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        status[2].setImageResource(results[2] ? R.drawable.done_drawable : R.drawable.failed_drawable);
                        test_progress.setProgress(3);

                        status[3].setImageResource(R.drawable.testing_drawable);
                        test_cur_cmd.setText(cmdReaver);
                    }
                });

                //Reaver
                Log.d("HIJACKER/test_thread", cmdReaver);
                Runtime.getRuntime().exec(cmdReaver);
                Thread.sleep(TEST_WAIT);

                if(getPIDs(PROCESS_REAVER).size()==0) results[3] = false;
                else{
                    stop(PROCESS_REAVER);
                    last_action = System.currentTimeMillis() + 10000;
                    results[3] = true;
                }
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        status[3].setImageResource(results[3] ? R.drawable.done_drawable : R.drawable.failed_drawable);
                        test_progress.setProgress(4);

                        status[4].setImageResource(R.drawable.testing_drawable);
                        test_cur_cmd.setText(R.string.checking_chroot);
                    }
                });

                //Chroot
                final int chroot_check = checkChroot();
                results[4] = chroot_check==CHROOT_FOUND;
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        if(chroot_check!=CHROOT_FOUND){
                            status[4].setImageResource(R.drawable.failed_drawable);
                            if(chroot_check==CHROOT_DIR_MISSING) test_cur_cmd.setText(R.string.chroot_notfound);
                            else if(chroot_check==CHROOT_BIN_MISSING) test_cur_cmd.setText(R.string.kali_notfound);
                            else test_cur_cmd.setText(R.string.chroot_both_notfound);
                        }else{
                            test_cur_cmd.setText(R.string.done);
                            status[4].setImageResource(R.drawable.done_drawable);
                        }
                        test_progress.setProgress(5);
                    }
                });

            }catch(IOException | InterruptedException e){
                Log.e("HIJACKER/test_thread", e.toString());
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        for(int i=0;i<status.length;i++){
                            status[i].setImageResource(results[i] ? R.drawable.done_drawable : R.drawable.failed_drawable);
                        }
                        test_progress.setProgress(5);
                    }
                });
            }finally{
                stop(PROCESS_AIRODUMP);
                stop(PROCESS_AIREPLAY);
                stop(PROCESS_MDK_BF);
                stop(PROCESS_REAVER);
            }
        }
    };
    public Dialog onCreateDialog(Bundle savedInstanceState){
        load();
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.test, null);

        test_progress = dialogView.findViewById(R.id.test_progress);
        status[0] = dialogView.findViewById(R.id.imageView1);
        status[1] = dialogView.findViewById(R.id.imageView2);
        status[2] = dialogView.findViewById(R.id.imageView3);
        status[3] = dialogView.findViewById(R.id.imageView4);
        status[4] = dialogView.findViewById(R.id.imageView5);
        test_cur_cmd = dialogView.findViewById(R.id.current_cmd);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        test_progress.setProgress(0);
        status[0].setImageResource(android.R.color.transparent);
        status[1].setImageResource(android.R.color.transparent);
        status[2].setImageResource(android.R.color.transparent);
        status[3].setImageResource(android.R.color.transparent);
        status[4].setImageResource(android.R.color.transparent);

        thread = new Thread(runnable);
        thread.start();

        builder.setView(dialogView);
        builder.setTitle(R.string.testing);
        builder.setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                thread.interrupt();
            }
        });
        builder.setNeutralButton(R.string.stop, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){}
        });
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!notif_on) super.show(fragmentManager, tag);
    }
    @Override
    public void onCancel(DialogInterface dialog){
        super.onCancel(dialog);
        thread.interrupt();
    }
    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            d.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    thread.interrupt();
                }
            });
        }
    }
}
