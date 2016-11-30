package com.hijacker;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.hijacker.MainActivity.FRAGMENT_REAVER;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.reaver_dir;
import static com.hijacker.MainActivity.stop;

public class ReaverFragment extends Fragment{
    static View v;
    static Button start_button, select_button;
    static TextView console;
    static Thread thread;
    static boolean cont;
    static String console_text = null, pin_delay="1", locked_delay="60";
    static boolean ignore_locked, eap_fail, small_dh;
    static AP ap=null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        v = inflater.inflate(R.layout.reaver_fragment, container, false);
        setRetainInstance(true);
        console = (TextView)v.findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());

        select_button = (Button)v.findViewById(R.id.select_ap);
        if(ap!=null) select_button.setText(ap.essid + " (" + ap.mac + ')');
        select_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                PopupMenu popup = new PopupMenu(getActivity(), view);

                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                for (int i = 0; i < AP.APs.size(); i++) {
                    popup.getMenu().add(0, i, i, AP.APs.get(i).essid + " (" + AP.APs.get(i).mac + ')');
                }
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        //ItemId = i in for()
                        AP temp = AP.APs.get(item.getItemId());
                        if(ap!=temp){
                            ap = temp;
                            stop.obtainMessage().sendToTarget();
                        }
                        select_button.setText(ap.essid + " (" + ap.mac + ')');
                        return true;
                    }
                });
                popup.show();
            }
        });

        if(thread==null){
            thread = new Thread(new Runnable(){
                @Override
                public void run(){
                    Log.d("ReaverFragment", "in thread");
                    try{
                        String cmd = "su -c " + prefix + " " + reaver_dir + " -i " + iface + " --channel " + ap.ch + " -b " + ap.mac + " -vvv";
                        cmd += " -d " + ((EditText)v.findViewById(R.id.pin_delay)).getText();
                        cmd += " -l " + ((EditText)v.findViewById(R.id.locked_delay)).getText();
                        if(((CheckBox)v.findViewById(R.id.ignore_locked)).isChecked()) cmd += " -L";
                        if(((CheckBox)v.findViewById(R.id.eap_fail)).isChecked()) cmd += " -E";
                        if(((CheckBox)v.findViewById(R.id.small_dh)).isChecked()) cmd += " -S";
                        if(debug) Log.d("ReaverFragment", cmd);
                        Process dc = Runtime.getRuntime().exec(cmd);
                        BufferedReader in = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                        cont = true;
                        String buffer;
                        while(cont && (buffer = in.readLine())!=null){
                            Message msg = new Message();
                            msg.obj = buffer;
                            refresh.sendMessage(msg);
                        }
                    }catch(IOException e){
                        Log.e("Exception", "Caught Exception in ReaverFragment: " + e.toString());
                    }

                    stop.obtainMessage().sendToTarget();
                }
            });
        }

        start_button = (Button)v.findViewById(R.id.start_button);
        start_button.setText(thread.isAlive() ? R.string.stop : R.string.start);
        start_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!thread.isAlive()){
                    if(ap==null){
                        Snackbar.make(v, select_button.getText(), Snackbar.LENGTH_LONG).show();
                    }else{
                        start_button.setText(R.string.stop);
                        stop(PROCESS_AIRODUMP);            //Can't have channels changing from anywhere else
                        progress.setIndeterminate(true);
                        thread.start();
                    }
                }else{
                    stop.obtainMessage().sendToTarget();
                }
            }
        });
        return v;
    }
    public static Handler stop = new Handler(){
        public void handleMessage(Message msg){
            start_button.setText(R.string.start);
            cont = false;
            progress.setIndeterminate(false);
            stop(PROCESS_REAVER);
        }
    };
    public static Handler refresh = new Handler(){
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
}

