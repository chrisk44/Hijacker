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
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.hijacker.MainActivity.FRAGMENT_CRACK;
import static com.hijacker.MainActivity.PROCESS_AIRCRACK;
import static com.hijacker.MainActivity.aircrack_dir;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.shell3_in;
import static com.hijacker.MainActivity.shell3_out;
import static com.hijacker.MainActivity.stop;

public class CrackFragment extends Fragment{
    static TextView console;
    static Button button;
    static View v;
    static Thread thread;
    static boolean cont;
    static String capfile, wordlist;
    static String cap_notfound, wordlist_notfound, select_wpa_wep;
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        v = inflater.inflate(R.layout.crack_fragment, container, false);
        console = (TextView)v.findViewById(R.id.console);
        console.setText("");
        console.setMovementMethod(new ScrollingMovementMethod());

        thread = new Thread(new Runnable(){
            @Override
            public void run(){
                Log.d("CrackFragment", "in thread");
                Process dc = null;
                try{
                    String cmd = "su -c " + aircrack_dir + " " + capfile + " -l " + path + "/aircrack-out.txt";
                    if(wordlist!=null) cmd += " -w " + wordlist;
                    if(debug) Log.d("CrackFragment", cmd);
                    dc = Runtime.getRuntime().exec(cmd);
                }catch(IOException e){ Log.e("Exception", "Caught Exception in CrackFragment: " + e.toString()); }
                BufferedReader in = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                try{
                    cont = true;
                    while(cont && in.readLine()!=null){}
                }catch(IOException e){ Log.e("Exception", "Caught Exception in _startAireplay() read block: " + e.toString()); }

                stop.obtainMessage().sendToTarget();
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
                if(!cap.exists() && cap.isFile()){
                    Snackbar.make(v, cap_notfound, Snackbar.LENGTH_LONG).show();
                }else if(!word.exists() && word.isFile()){
                    Snackbar.make(v, wordlist_notfound, Snackbar.LENGTH_LONG).show();
                }else if(thread.isAlive()){
                    stop.obtainMessage().sendToTarget();
                }else{
                    switch(((RadioGroup)v.findViewById(R.id.radio_group)).getCheckedRadioButtonId()){
                        case R.id.wpa_rb:
                            //WPA

                            button.setText(R.string.stop);
                            console.append("Running...\n");
                            progress.setIndeterminate(true);
                            thread.start();
                            break;
                        case R.id.wep_rb:
                            //WEP
                            break;
                        default:
                            Snackbar.make(v, select_wpa_wep, Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return v;
    }
    public static Handler stop = new Handler(){
        public void handleMessage(Message msg){
            button.setText(R.string.start);
            cont = false;
            progress.setIndeterminate(false);
            stop(PROCESS_AIRCRACK);
            if((new File(path + "/aircrack-out.txt")).exists()){
                shell3_in.print("cat " + path + "/aircrack-out.txt; echo \n");
                shell3_in.flush();
                try{
                    console.append("Key found: " + shell3_out.readLine() + '\n');
                }catch(IOException ignored){}
                shell3_in.print("rm " + path + "/aircrack-out.txt\n");
                shell3_in.flush();
            }else{
                console.append("Key not found\n");
            }

        }
    };
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_CRACK;
    }
}
