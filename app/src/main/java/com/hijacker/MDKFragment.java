package com.hijacker;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import static com.hijacker.MainActivity.FRAGMENT_MDK;
import static com.hijacker.MainActivity.MDK_ADOS;
import static com.hijacker.MainActivity.MDK_BF;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.startMdk;
import static com.hijacker.MainActivity.stop;

public class MDKFragment extends Fragment{
    static boolean bf=false, ados=false;
    static int bf_pid, ados_pid;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View view = inflater.inflate(R.layout.mdk_fragment, container, false);

        Switch temp = (Switch)view.findViewById(R.id.bf_switch);
        temp.setChecked(bf);
        temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                //Beacon Flooding
                if(b){
                    String ssid_file = ((EditText)view.findViewById(R.id.ssid_file)).getText().toString();
                    if(ssid_file.equals("")){
                        startMdk(MDK_BF, null);
                    }else{
                        startMdk(MDK_BF, ssid_file);
                    }
                    if(debug) Log.d("MDKFragment", "bf_pid is " + bf_pid);
                }else{
                    bf = false;
                    stop(bf_pid);
                }
            }
        });
        temp = (Switch)view.findViewById(R.id.ados_switch);
        temp.setChecked(ados);
        temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                //Authentication DoS
                if(b){
                    startMdk(MDK_ADOS, null);
                    if(debug) Log.d("MDKFragment", "ados_pid is " + ados_pid);
                }else{
                    ados = false;
                    stop(ados_pid);
                }
            }
        });

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_MDK;
    }
}

