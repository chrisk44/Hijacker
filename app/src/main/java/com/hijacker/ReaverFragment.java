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

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import static android.widget.Toast.LENGTH_SHORT;
import static com.hijacker.CustomAPDialog.FOR_REAVER;
import static com.hijacker.MainActivity.FRAGMENT_REAVER;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.cont_on_fail;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.custom_chroot_cmd;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.monstart;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.reaver_dir;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.Shell.runOne;

public class ReaverFragment extends Fragment{
    View v;
    static Button start_button, select_button;
    TextView console;
    static Thread thread;
    static Runnable runnable;
    static boolean cont=false;
    static String console_text = null, pin_delay="1", locked_delay="60", custom_mac=null;       //delays are always used as strings
    static boolean ignore_locked, eap_fail, small_dh;
    static AP ap=null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        v = inflater.inflate(R.layout.reaver_fragment, container, false);
        setRetainInstance(true);
        console = (TextView)v.findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());

        runOne("chmod a+r " + MainActivity.chroot_dir);
        File chroot_dir = new File(MainActivity.chroot_dir);
        boolean kali_init = false;
        try{
            Process dc = Runtime.getRuntime().exec("ls /system/bin -1 | grep bootkali_init");
            BufferedReader out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
            kali_init = out.readLine()!=null;
        }catch(IOException ignored){}
        if(debug){
            Log.d("ReaverFragment", "chroot_dir is " + Boolean.toString(chroot_dir.exists()));
            Log.d("ReaverFragment", "kali_init is " + Boolean.toString(kali_init));
        }
        if(!chroot_dir.exists() || !kali_init){
            v.findViewById(R.id.pixie_dust).setEnabled(false);
            if(!chroot_dir.exists()) Toast.makeText(getActivity(), getString(R.string.chroot_notfound), LENGTH_SHORT).show();
            if(!kali_init) Toast.makeText(getActivity(), getString(R.string.kali_notfound), LENGTH_SHORT).show();
        }

        select_button = (Button)v.findViewById(R.id.select_ap);
        if(custom_mac!=null) select_button.setText(custom_mac);
        else if(ap!=null) select_button.setText(ap.essid + " (" + ap.mac + ')');
        select_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                PopupMenu popup = new PopupMenu(getActivity(), view);

                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                int i;
                for (i = 0; i < AP.APs.size(); i++) {
                    popup.getMenu().add(0, i, i, AP.APs.get(i).essid + " (" + AP.APs.get(i).mac + ')');
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
                                stop.obtainMessage().sendToTarget();
                            }
                            select_button.setText(ap.essid + " (" + ap.mac + ')');
                        }else{
                            //Clcked custom
                            CustomAPDialog dialog = new CustomAPDialog();
                            dialog.mode = FOR_REAVER;
                            dialog.show(getFragmentManager(), "CustomAPDialog");
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
                Log.d("ReaverFragment", "in thread");
                try{
                    BufferedReader out;
                    Message msg;
                    String args = "-i " + iface + " -vv";
                    args += ap==null ? " -b " + custom_mac : " -b " + ap.mac + " --channel " + ap.ch;
                    args += " -d " + ((EditText)v.findViewById(R.id.pin_delay)).getText();
                    args += " -l " + ((EditText)v.findViewById(R.id.locked_delay)).getText();
                    if(((CheckBox)v.findViewById(R.id.ignore_locked)).isChecked()) args += " -L";
                    if(((CheckBox)v.findViewById(R.id.eap_fail)).isChecked()) args += " -E";
                    if(((CheckBox)v.findViewById(R.id.small_dh)).isChecked()) args += " -S";
                    String cmd;
                    if(((CheckBox)v.findViewById(R.id.pixie_dust)).isChecked()){
                        msg = new Message();
                        msg.obj = getString(R.string.chroot_warning);
                        refresh.sendMessage(msg);
                        Thread.sleep(3000);
                        Runtime.getRuntime().exec("su -c bootkali_init");       //Make sure kali has booted
                        args += " -K 1";
                        cmd = "chroot /data/local/nhsystem/kali-armhf /bin/bash -c \'" + get_chroot_env() + "reaver " + args + "\'";
                        msg = new Message();
                        msg.obj = "\nRunning: " + cmd + '\n';
                        refresh.sendMessage(msg);
                        ProcessBuilder pb = new ProcessBuilder("su");
                        pb.redirectErrorStream(true);
                        Process dc = pb.start();
                        out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                        PrintWriter in = new PrintWriter(dc.getOutputStream());
                        in.print(cmd + "\nexit\n");
                        in.flush();
                    }else{
                        cmd = "su -c " + prefix + " " + reaver_dir + " " + args;
                        msg = new Message();
                        msg.obj = "Running: " + cmd;
                        refresh.sendMessage(msg);
                        Process dc = Runtime.getRuntime().exec(cmd);
                        out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                    }
                    if(debug) Log.d("ReaverFragment", cmd);
                    cont = true;
                    String buffer;
                    while(cont && (buffer = out.readLine())!=null){
                        msg = new Message();
                        msg.obj = buffer;
                        refresh.sendMessage(msg);
                    }
                    msg = new Message();
                    msg.obj = "\nDone\n";
                    refresh.sendMessage(msg);
                }catch(IOException | InterruptedException e){
                    Log.e("Exception", "Caught Exception in ReaverFragment: " + e.toString());
                }

                stop.obtainMessage().sendToTarget();
            }
        };
        thread = new Thread(runnable);

        start_button = (Button)v.findViewById(R.id.start_button);
        start_button.setText(thread.isAlive() ? R.string.stop : R.string.start);
        start_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!thread.isAlive()){
                    if(ap==null && custom_mac==null){
                        Snackbar.make(v, select_button.getText(), Snackbar.LENGTH_LONG).show();
                    }else{
                        start_button.setText(R.string.stop);
                        stop(PROCESS_AIRODUMP);            //Can't have channels changing from anywhere else
                        progress.setIndeterminate(true);
                        thread = new Thread(runnable);
                        thread.start();
                    }
                }else{
                    stop.obtainMessage().sendToTarget();
                }
            }
        });
        return v;
    }
    public Handler stop = new Handler(){
        public void handleMessage(Message msg){
            start_button.setText(R.string.start);
            cont = false;
            progress.setIndeterminate(false);
            stop(PROCESS_REAVER);
        }
    };
    public Handler refresh = new Handler(){
        public void handleMessage(Message msg){
            console.append((String)msg.obj + '\n');
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_REAVER;
        console.setText(console_text);
        ((EditText)v.findViewById(R.id.pin_delay)).setText(pin_delay);
        ((EditText)v.findViewById(R.id.locked_delay)).setText(locked_delay);
        ((CheckBox)v.findViewById(R.id.ignore_locked)).setChecked(ignore_locked);
        ((CheckBox)v.findViewById(R.id.eap_fail)).setChecked(eap_fail);
        ((CheckBox)v.findViewById(R.id.small_dh)).setChecked(small_dh);
    }
    @Override
    public void onPause(){
        super.onPause();
        console_text = console.getText().toString();
        pin_delay = ((EditText)v.findViewById(R.id.pin_delay)).getText().toString();
        locked_delay = ((EditText)v.findViewById(R.id.locked_delay)).getText().toString();
        ignore_locked = ((CheckBox)v.findViewById(R.id.ignore_locked)).isChecked();
        eap_fail = ((CheckBox)v.findViewById(R.id.eap_fail)).isChecked();
        small_dh = ((CheckBox)v.findViewById(R.id.small_dh)).isChecked();
    }
    static String get_chroot_env(){
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
            ENV_OUT += custom_chroot_cmd;
            ENV_OUT += cont_on_fail ? "; " : " && ";
        }
        return ENV_OUT;
    }
}

