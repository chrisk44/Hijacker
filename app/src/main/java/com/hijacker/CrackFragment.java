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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.hijacker.MainActivity.FRAGMENT_CRACK;
import static com.hijacker.MainActivity.PROCESS_AIRCRACK;
import static com.hijacker.MainActivity.aircrack_dir;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getLastLine;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.stop;

public class CrackFragment extends Fragment{
    static final int WPA=2, WEP=1;
    TextView console;
    Button button;
    static int mode;
    static Thread thread;
    static Runnable runnable;
    static boolean cont=false;
    static String capfile, wordlist, console_text;
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        final View v = inflater.inflate(R.layout.crack_fragment, container, false);
        console = (TextView)v.findViewById(R.id.console);
        console.setText("");
        console.setMovementMethod(new ScrollingMovementMethod());

        final RadioGroup wep_rg = (RadioGroup)v.findViewById(R.id.wep_rg);
        for (int i = 0; i < wep_rg.getChildCount(); i++) {
            //Disable all the WEP options
            wep_rg.getChildAt(i).setEnabled(false);
        }

        Shell shell = Shell.getFreeShell();
        shell.run("busybox ls -1 " + cap_dir + "/handshake-*.cap; echo ENDOFLS");
        capfile = getLastLine(shell.getShell_out(), "ENDOFLS");
        if(!capfile.equals("ENDOFLS") && capfile.charAt(0)!='l'){
            ((EditText)v.findViewById(R.id.capfile)).setText(capfile);
        }
        shell.done();

        runnable = new Runnable(){
            @Override
            public void run(){
                Log.d("CrackFragment", "in thread");
                try{
                    String cmd = "su -c " + aircrack_dir + " " + capfile + " -l " + path + "/aircrack-out.txt -a " + mode;
                    if(wordlist!=null) cmd += " -w " + wordlist;
                    if(mode==WEP){
                        cmd += " -n ";
                        switch(wep_rg.getCheckedRadioButtonId()){
                            case R.id.wep_64:
                                cmd += "64";
                                break;
                            case R.id.wep_128:
                                cmd += "128";
                                break;
                            case R.id.wep_152:
                                cmd += "152";
                                break;
                            case R.id.wep_256:
                                cmd += "256";
                                break;
                            case R.id.wep_512:
                                cmd += "512";
                                break;
                        }
                    }
                    if(debug) Log.d("CrackFragment", cmd);
                    Process dc = Runtime.getRuntime().exec(cmd);
                    BufferedReader out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                    cont = true;
                    while(cont && out.readLine()!=null){
                        Thread.sleep(100);
                    }
                }catch(IOException | InterruptedException e){ Log.e("Exception", "Caught Exception in CrackFragment: " + e.toString()); }

                stop.obtainMessage().sendToTarget();
            }
        };
        thread = new Thread(runnable);

        ((RadioButton)v.findViewById(R.id.wep_rb)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                for (int i = 0; i < wep_rg.getChildCount(); i++) {
                    //If wep is now checked, enable the wep options, otherwise disable them
                    wep_rg.getChildAt(i).setEnabled(b);
                }
            }
        });
        button = (Button)v.findViewById(R.id.start);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                capfile = ((EditText)v.findViewById(R.id.capfile)).getText().toString();
                wordlist = ((EditText)v.findViewById(R.id.wordlist)).getText().toString();
                File cap = new File(capfile);
                File word = new File(wordlist);
                if(thread.isAlive()){
                    cont = false;
                }else if(!cap.exists() || !cap.isFile()){
                    Snackbar.make(v, getString(R.string.cap_notfound), Snackbar.LENGTH_LONG).show();
                }else if(!word.exists() || !word.isFile()){
                    Snackbar.make(v, getString(R.string.wordlist_notfound), Snackbar.LENGTH_LONG).show();
                }else{
                    RadioGroup temp = (RadioGroup)v.findViewById(R.id.radio_group);
                    if(temp.getCheckedRadioButtonId()==-1){
                        //Mode not selected
                        Snackbar.make(v, getString(R.string.select_wpa_wep), Snackbar.LENGTH_SHORT).show();
                    }else if(temp.getCheckedRadioButtonId()==R.id.wep_rb &&
                            ((RadioGroup)v.findViewById(R.id.wep_rg)).getCheckedRadioButtonId()==-1){
                        //If wep is selected, we need to have a wep bit length selection
                        Snackbar.make(v, getString(R.string.select_wep_bits), Snackbar.LENGTH_SHORT).show();
                    }else{
                        switch(((RadioGroup) v.findViewById(R.id.radio_group)).getCheckedRadioButtonId()){
                            case R.id.wpa_rb:
                                //WPA
                                mode = WPA;
                                break;
                            case R.id.wep_rb:
                                //WEP
                                mode = WEP;
                                break;
                        }
                        button.setText(R.string.stop);
                        console.append("\nRunning...\n");
                        progress.setIndeterminate(true);
                        thread = new Thread(runnable);
                        thread.start();
                    }
                }
            }
        });

        return v;
    }
    public Handler stop = new Handler(){
        public void handleMessage(Message msg){
            button.setText(R.string.start);
            cont = false;
            progress.setIndeterminate(false);
            stop(PROCESS_AIRCRACK);
            if(new File(path + "/aircrack-out.txt").exists()){
                Shell shell = Shell.getFreeShell();
                BufferedReader out = shell.getShell_out();
                shell.run("cat " + path + "/aircrack-out.txt; echo ");              //No newline at the end of the file, readLine will hang
                try{
                    console.append("Key found: " + out.readLine() + '\n');
                }catch(IOException ignored){}
                shell.run("rm " + path + "/aircrack-out.txt");
                shell.done();
            }else{
                console.append("Key not found\n");
                if(mode==WEP) console.append("Try with different wep bit selection or more IVs\n");
            }
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_CRACK;
        console.setText(console_text);
    }
    @Override
    public void onPause(){
        super.onPause();
        console_text = console.getText().toString();
    }
}
