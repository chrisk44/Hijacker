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
import android.os.Environment;
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
import static com.hijacker.MainActivity.busybox;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getLastLine;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.stop;

public class CrackFragment extends Fragment{
    static final int WPA=2, WEP=1;
    View fragmentView;
    TextView console;
    EditText cap_et, wordlist_et;
    Button button;
    static int mode;
    static Thread thread;
    static Runnable runnable;
    static boolean cont=false;
    static String capfile, wordlist, console_text, capfile_text=null, wordlist_text=null;
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        fragmentView = inflater.inflate(R.layout.crack_fragment, container, false);
        console = (TextView)fragmentView.findViewById(R.id.console);
        console.setText("");
        console.setMovementMethod(new ScrollingMovementMethod());

        cap_et = (EditText)fragmentView.findViewById(R.id.capfile);
        wordlist_et = (EditText)fragmentView.findViewById(R.id.wordlist);

        final RadioGroup wep_rg = (RadioGroup)fragmentView.findViewById(R.id.wep_rg);
        for (int i = 0; i < wep_rg.getChildCount(); i++) {
            //Disable all the WEP options
            wep_rg.getChildAt(i).setEnabled(false);
        }

        runnable = new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("HIJACKER/CrackFragment", "in thread");
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
                    if(debug) Log.d("HIJACKER/CrackFragment", cmd);
                    Process dc = Runtime.getRuntime().exec(cmd);
                    BufferedReader out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                    cont = true;
                    while(cont && out.readLine()!=null){
                        Thread.sleep(100);
                    }
                }catch(IOException | InterruptedException e){ Log.e("HIJACKER/Exception", "Caught Exception in CrackFragment: " + e.toString()); }

                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        button.setText(R.string.start);
                        cont = false;
                        progress.setIndeterminate(false);
                        stop(PROCESS_AIRCRACK);
                        if(new File(path + "/aircrack-out.txt").exists()){
                            Shell shell = Shell.getFreeShell();     //Using root shell because "new File(path + "/aircrack-out.txt");" throws FileNotFoundException (permission denied)
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
                });
            }
        };
        thread = new Thread(runnable);

        ((RadioButton)fragmentView.findViewById(R.id.wep_rb)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                for (int i = 0; i < wep_rg.getChildCount(); i++) {
                    //If wep is now checked, enable the wep options, otherwise disable them
                    wep_rg.getChildAt(i).setEnabled(b);
                }
            }
        });
        button = (Button)fragmentView.findViewById(R.id.start);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                capfile = cap_et.getText().toString();
                wordlist = wordlist_et.getText().toString();
                if(!capfile.startsWith("/")){
                    Snackbar.make(fragmentView, getString(R.string.capfile_invalid), Snackbar.LENGTH_LONG).show();
                    return;
                }
                if(!wordlist.startsWith("/")){
                    Snackbar.make(fragmentView, getString(R.string.wordlist_invalid), Snackbar.LENGTH_LONG).show();
                    return;
                }
                RootFile cap = new RootFile(capfile);
                RootFile word = new RootFile(wordlist);
                if(thread.isAlive()){
                    cont = false;
                }else if(!cap.exists() || !cap.isFile()){
                    Snackbar.make(fragmentView, getString(R.string.cap_notfound), Snackbar.LENGTH_LONG).show();
                }else if(!word.exists() || !word.isFile()){
                    Snackbar.make(fragmentView, getString(R.string.wordlist_notfound), Snackbar.LENGTH_LONG).show();
                }else{
                    RadioGroup temp = (RadioGroup)fragmentView.findViewById(R.id.radio_group);
                    if(temp.getCheckedRadioButtonId()==-1){
                        //Mode not selected
                        Snackbar.make(fragmentView, getString(R.string.select_wpa_wep), Snackbar.LENGTH_SHORT).show();
                    }else if(temp.getCheckedRadioButtonId()==R.id.wep_rb &&
                            ((RadioGroup)fragmentView.findViewById(R.id.wep_rg)).getCheckedRadioButtonId()==-1){
                        //If wep is selected, we need to have a wep bit length selection
                        Snackbar.make(fragmentView, getString(R.string.select_wep_bits), Snackbar.LENGTH_SHORT).show();
                    }else{
                        switch(temp.getCheckedRadioButtonId()){
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
        fragmentView.findViewById(R.id.cap_fe_btn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setStartingDir(new RootFile(cap_dir));
                dialog.setToSelect(FileExplorerDialog.SELECT_EXISTING_FILE);
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        cap_et.setText(dialog.result.getAbsolutePath());
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
            }
        });
        fragmentView.findViewById(R.id.wordlist_fe_btn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setStartingDir(new RootFile(Environment.getExternalStorageDirectory().toString()));
                dialog.setToSelect(FileExplorerDialog.SELECT_EXISTING_FILE);
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        wordlist_et.setText(dialog.result.getAbsolutePath());
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
            }
        });

        return fragmentView;
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_CRACK;
        if(capfile_text!=null){
            ((EditText)fragmentView.findViewById(R.id.capfile)).setText(capfile_text);
        }else{
            Shell shell = Shell.getFreeShell();
            shell.run(busybox + " ls -1 " + cap_dir + "/handshake-*.cap; echo ENDOFLS");
            capfile = getLastLine(shell.getShell_out(), "ENDOFLS");
            if(!capfile.equals("ENDOFLS") && capfile.charAt(0)!='l'){
                ((EditText)fragmentView.findViewById(R.id.capfile)).setText(capfile);
            }
            shell.done();
        }
        if(wordlist_text!=null) ((EditText)fragmentView.findViewById(R.id.wordlist)).setText(wordlist_text);
        console.setText(console_text);
        refreshDrawer();
    }
    @Override
    public void onPause(){
        super.onPause();
        console_text = console.getText().toString();
        capfile_text = cap_et.getText().toString();
        wordlist_text = wordlist_et.getText().toString();
    }
}
