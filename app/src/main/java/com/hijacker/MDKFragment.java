package com.hijacker;


import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import static com.hijacker.MainActivity.FRAGMENT_MDK;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getPIDs;
import static com.hijacker.MainActivity.startMdk;
import static com.hijacker.MainActivity.stop;

public class MDKFragment extends Fragment{
    static boolean bf=false, ados=false;
    static int bf_pid, ados_pid;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.mdk_fragment, container, false);

        Switch temp = (Switch)view.findViewById(R.id.bf_switch);
        temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                if(b){
                    startMdk(0, null);
                    bf = true;
                    try{
                        Thread.sleep(500);
                    }catch(InterruptedException ignored){}
                    //If ADoS is running, then the bf pid is the second mdk3 process, otherwise it's the first
                    bf_pid = getPIDs(2).get(ados ? 1 : 0);
                    if(debug) Log.d("MDKFragment", "bf_pid is " + bf_pid);
                }else{
                    stop(bf_pid);
                    bf = false;
                }
            }
        });
        temp.setChecked(bf);
        temp = (Switch)view.findViewById(R.id.ados_switch);
        temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                if(b){
                    startMdk(1, null);
                    ados = true;
                    try{
                        Thread.sleep(500);
                    }catch(InterruptedException ignored){}
                    //If bf is running, then the ados pid is the second mdk3 process, otherwise it's the first
                    ados_pid = getPIDs(2).get(bf ? 1 : 0);
                    if(debug) Log.d("MDKFragment", "ados_pid is " + ados_pid);
                }else{
                    stop(ados_pid);
                    ados = false;
                }
            }
        });
        temp.setChecked(ados);

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_MDK;
    }
}

