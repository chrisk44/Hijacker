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

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import static android.widget.Toast.LENGTH_SHORT;
import static com.hijacker.MainActivity.CHROOT_BIN_MISSING;
import static com.hijacker.MainActivity.CHROOT_DIR_MISSING;
import static com.hijacker.MainActivity.CHROOT_FOUND;
import static com.hijacker.MainActivity.FRAGMENT_REAVER;
import static com.hijacker.MainActivity.NETHUNTER_BOOTKALI_BASH;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.bootkali_init_bin;
import static com.hijacker.MainActivity.checkChroot;
import static com.hijacker.MainActivity.cont_on_fail;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.custom_chroot_cmd;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.last_action;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.monstart;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.reaver_dir;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.stop;

public class ReaverFragment extends Fragment{
    View fragmentView;
    static Button start_button, select_button;
    TextView console;
    EditText pin_delay_et, locked_delay_et;
    CheckBox pixie_dust_et, ignored_locked_cb, eap_fail_cb, small_dh_cb;
    static Thread thread;
    static Runnable runnable;
    static boolean cont=false;
    static String console_text = null, pin_delay="1", locked_delay="60", custom_mac=null;       //delays are always used as strings
    static boolean pixie_dust, ignore_locked, eap_fail, small_dh;
    static AP ap=null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        fragmentView = inflater.inflate(R.layout.reaver_fragment, container, false);
        setRetainInstance(true);

        console = (TextView)fragmentView.findViewById(R.id.console);
        pin_delay_et = (EditText)fragmentView.findViewById(R.id.pin_delay);
        locked_delay_et = (EditText)fragmentView.findViewById(R.id.locked_delay);
        pixie_dust_et = (CheckBox)fragmentView.findViewById(R.id.pixie_dust);
        ignored_locked_cb = (CheckBox)fragmentView.findViewById(R.id.ignore_locked);
        eap_fail_cb = (CheckBox)fragmentView.findViewById(R.id.eap_fail);
        small_dh_cb = (CheckBox)fragmentView.findViewById(R.id.small_dh);
        select_button = (Button)fragmentView.findViewById(R.id.select_ap);
        start_button = (Button)fragmentView.findViewById(R.id.start_button);

        console.setMovementMethod(new ScrollingMovementMethod());

        int chroot_check = checkChroot();
        if(chroot_check!=CHROOT_FOUND){
            pixie_dust_et.setEnabled(false);
            if(chroot_check==CHROOT_DIR_MISSING) Toast.makeText(getActivity(), getString(R.string.chroot_notfound), LENGTH_SHORT).show();
            else if(chroot_check==CHROOT_BIN_MISSING) Toast.makeText(getActivity(), getString(R.string.kali_notfound), LENGTH_SHORT).show();
            else Toast.makeText(getActivity(), getString(R.string.chroot_both_notfound), LENGTH_SHORT).show();
        }

        select_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                PopupMenu popup = new PopupMenu(getActivity(), view);

                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                int i;
                for (i = 0; i < AP.APs.size(); i++) {
                    popup.getMenu().add(0, i, i, AP.APs.get(i).toString());
                }
                popup.getMenu().add(1, i, i, "Custom");
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        //ItemId = i in for()
                        if(item.getGroupId()==0){
                            custom_mac = null;
                            AP temp = AP.APs.get(item.getItemId());
                            if(ap!=temp){
                                ap = temp;
                                runInHandler(stopRunnable);
                            }
                            select_button.setText(ap.toString());
                        }else{
                            //Clcked custom
                            final EditTextDialog dialog = new EditTextDialog();
                            dialog.setTitle(getString(R.string.custom_ap_title));
                            dialog.setHint(getString(R.string.mac_address));
                            dialog.setRunnable(new Runnable(){
                                @Override
                                public void run(){
                                    ap = null;
                                    custom_mac = dialog.result;
                                    select_button.setText(dialog.result);
                                }
                            });
                            dialog.show(mFragmentManager, "EditTextDialog");
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        runnable = new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("HIJACKER/ReaverFragment", "in thread");
                try{
                    BufferedReader out;
                    String args = "-i " + iface + " -vv";
                    args += ap==null ? " -b " + custom_mac : " -b " + ap.mac + " --channel " + ap.ch;
                    args += " -d " + pin_delay_et.getText();
                    args += " -l " + locked_delay_et.getText();
                    if(ignored_locked_cb.isChecked()) args += " -L";
                    if(eap_fail_cb.isChecked()) args += " -E";
                    if(small_dh_cb.isChecked()) args += " -S";
                    String cmd;
                    if(pixie_dust_et.isChecked()){
                        writeToConsole(getString(R.string.chroot_warning));
                        if(bootkali_init_bin.equals(NETHUNTER_BOOTKALI_BASH)){
                            //Not in nethunter, need to initialize the chroot environment
                            Runtime.getRuntime().exec("su -c " + bootkali_init_bin);       //Make sure kali has booted
                        }
                        args += " -K 1";
                        cmd = "chroot " + MainActivity.chroot_dir + " /bin/bash -c \'" + get_chroot_env(getActivity()) + "reaver " + args + "\'";
                        writeToConsole("\nRunning: " + cmd + '\n');
                        ProcessBuilder pb = new ProcessBuilder("su");
                        pb.redirectErrorStream(true);
                        Process dc = pb.start();
                        out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                        PrintWriter in = new PrintWriter(dc.getOutputStream());
                        in.print(cmd + "\nexit\n");
                        in.flush();
                    }else{
                        cmd = "su -c " + prefix + " " + reaver_dir + " " + args;
                        writeToConsole("Running: " + cmd);
                        Process dc = Runtime.getRuntime().exec(cmd);
                        out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                    }
                    last_action = System.currentTimeMillis();
                    if(debug) Log.d("HIJACKER/ReaverFragment", cmd);
                    cont = true;
                    String buffer;
                    while(cont && (buffer = out.readLine())!=null){
                        writeToConsole(buffer);
                    }
                    writeToConsole("\nDone\n");
                }catch(IOException e){
                    Log.e("HIJACKER/Exception", "Caught Exception in ReaverFragment: " + e.toString());
                }

                runInHandler(stopRunnable);
            }
        };
        if(thread==null) thread = new Thread(runnable);

        start_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!thread.isAlive()){
                    if(ap==null && custom_mac==null){
                        Snackbar.make(fragmentView, select_button.getText(), Snackbar.LENGTH_LONG).show();
                    }else{
                        if(pin_delay_et.getText().toString().equals("")){
                            Snackbar.make(fragmentView, getString(R.string.pin_delay_empty), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        if(locked_delay_et.getText().toString().equals("")){
                            Snackbar.make(fragmentView, getString(R.string.locked_delay_empty), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        start_button.setText(R.string.stop);
                        cont = true;
                        stop(PROCESS_AIRODUMP);            //Can't have channels changing from anywhere else
                        progress.setIndeterminate(true);
                        thread = new Thread(runnable);
                        thread.start();
                    }
                }else{
                    runInHandler(stopRunnable);
                }
            }
        });
        return fragmentView;
    }
    Runnable stopRunnable = new Runnable(){
        @Override
        public void run(){
            start_button.setText(R.string.start);
            cont = false;
            progress.setIndeterminate(false);
            stop(PROCESS_REAVER);
        }
    };
    void writeToConsole(final String str){
        if(currentFragment==FRAGMENT_REAVER && !background){
            runInHandler(new Runnable(){
                @Override
                public void run(){
                    console.append(str + '\n');
                }
            });
        }else{
            console_text += str + '\n';
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_REAVER;
        refreshDrawer();
        console.setText(console_text);
        pin_delay_et.setText(pin_delay);
        locked_delay_et.setText(locked_delay);
        pixie_dust_et.setChecked(pixie_dust);
        ignored_locked_cb.setChecked(ignore_locked);
        eap_fail_cb.setChecked(eap_fail);
        small_dh_cb.setChecked(small_dh);
        if(custom_mac!=null) select_button.setText(custom_mac);
        else if(ap!=null) select_button.setText(ap.toString());
        else if(!AP.marked.isEmpty()){
            ap = AP.marked.get(AP.marked.size()-1);
            select_button.setText(ap.toString());
        }
        start_button.setText(thread.isAlive() ? R.string.stop : R.string.start);
    }
    @Override
    public void onPause(){
        super.onPause();
        console_text = console.getText().toString();
        pin_delay = pin_delay_et.getText().toString();
        locked_delay = locked_delay_et.getText().toString();
        pixie_dust = pixie_dust_et.isChecked();
        ignore_locked = ignored_locked_cb.isChecked();
        eap_fail = eap_fail_cb.isChecked();
        small_dh = small_dh_cb.isChecked();
    }
    static String get_chroot_env(final Activity activity){
        // add strings here , they will be in the kali env
        String[] ENV = {
                "USER=root",
                "SHELL=/bin/bash",
                "MAIL=/var/mail/root",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "TERM=linux",
                "HOME=/root",
                "LOGNAME=root",
                "SHLVL=1",
                "YOU_KNOW_WHAT=THIS_IS_KALI_LINUX_NETHUNER_FROM_JAVA_BINKY"
        };
        String ENV_OUT = "";
        for (String aENV : ENV) {
            ENV_OUT = ENV_OUT + "export " + aENV + " && ";
        }
        if(monstart){
            ENV_OUT += "source monstart-nh";
            ENV_OUT += cont_on_fail ? "; " : " && ";
        }
        if(!custom_chroot_cmd.equals("")){
            if(custom_chroot_cmd.contains("'") && activity!=null){
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        Toast.makeText(activity, activity.getString(R.string.custom_chroot_cmd_illegal), Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                ENV_OUT += custom_chroot_cmd;
                ENV_OUT += cont_on_fail ? "; " : " && ";
            }
        }
        return ENV_OUT;
    }
}

