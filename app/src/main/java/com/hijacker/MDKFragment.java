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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;

import static com.hijacker.CustomAPDialog.FOR_MDK;
import static com.hijacker.MainActivity.FRAGMENT_MDK;
import static com.hijacker.MainActivity.MDK_ADOS;
import static com.hijacker.MainActivity.MDK_BF;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.startMdk;
import static com.hijacker.MainActivity.stop;

public class MDKFragment extends Fragment{
    View v;
    static AP ados_ap=null;
    static Switch bf_switch, ados_switch;
    static Button select_button;
    static String custom_mac=null, ssid_file=null;
    static boolean managed=true, adhoc=true, opn=true, wep=true, tkip=true, aes=true;
    static boolean bf=false, ados=false;
    static int bf_pid, ados_pid;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        setRetainInstance(true);
        v = inflater.inflate(R.layout.mdk_fragment, container, false);

        bf_switch = (Switch)v.findViewById(R.id.bf_switch);
        bf_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                //Beacon Flooding
                if(b){
                    String ssid_file = ((EditText)v.findViewById(R.id.ssid_file)).getText().toString();
                    managed = ((CheckBox)v.findViewById(R.id.managed)).isChecked();
                    adhoc = ((CheckBox)v.findViewById(R.id.adhoc)).isChecked();
                    opn = ((CheckBox)v.findViewById(R.id.opn)).isChecked();
                    wep = ((CheckBox)v.findViewById(R.id.wep)).isChecked();
                    tkip = ((CheckBox)v.findViewById(R.id.tkip)).isChecked();
                    aes = ((CheckBox)v.findViewById(R.id.aes)).isChecked();
                    String args = "";
                    if(!managed && !adhoc){
                        Toast.makeText(getActivity(), getString(R.string.select_type), Toast.LENGTH_SHORT).show();
                        bf_switch.setChecked(false);
                        return;
                    }
                    if(!(managed && adhoc)){
                        if(managed) args += " -t 0";
                        if(adhoc) args += " -t 1";
                    }
                    if(!(opn || wep || tkip || aes)){
                        Toast.makeText(getActivity(), getString(R.string.select_enc), Toast.LENGTH_SHORT).show();
                        bf_switch.setChecked(false);
                        return;
                    }
                    args += " -w ";
                    if(opn) args += 'n';
                    if(wep) args += 'w';
                    if(tkip) args += 't';
                    if(aes) args += 'a';
                    if(!ssid_file.equals("")){
                        if(!(new File(ssid_file).exists())){
                            Toast.makeText(getActivity(), ssid_file + " doesn't exist", Toast.LENGTH_SHORT).show();
                            bf_switch.setChecked(false);
                            return;
                        }else{
                            args += " -f " + ssid_file;
                        }
                    }
                    startMdk(MDK_BF, args);
                    if(debug) Log.d("HIJACKER/MDKFragment", "bf_pid is " + bf_pid);
                }else{
                    bf = false;
                    stop(bf_pid);
                }
            }
        });
        ados_switch = (Switch)v.findViewById(R.id.ados_switch);
        ados_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                //Authentication DoS
                if(b){
                    startMdk(MDK_ADOS, ados_ap==null ? custom_mac : ados_ap.mac);
                    if(debug) Log.d("HIJACKER/MDKFragment", "ados_pid is " + ados_pid);
                }else{
                    ados = false;
                    stop(ados_pid);
                }
            }
        });
        select_button = (Button)v.findViewById(R.id.select_ap_ados);
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
                            custom_mac=null;
                            AP temp = AP.APs.get(item.getItemId());
                            if(ados_ap!=temp){
                                ados_ap = temp;
                                runInHandler(new Runnable(){
                                    @Override
                                    public void run(){
                                        ados_switch.setChecked(false);
                                        stop(ados_pid);
                                    }
                                });
                            }
                            select_button.setText(ados_ap.essid + " (" + ados_ap.mac + ')');
                        }else{
                            //Clcked custom
                            CustomAPDialog dialog = new CustomAPDialog();
                            dialog.mode = FOR_MDK;
                            dialog.show(mFragmentManager, "CustomAPDialog");
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        return v;
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_MDK;
        //Restore options
        bf_switch.setChecked(bf);
        ados_switch.setChecked(ados);
        if(custom_mac!=null) select_button.setText(custom_mac);
        else if(ados_ap!=null) select_button.setText(ados_ap.essid + " (" + ados_ap.mac + ')');
        else if(!AP.marked.isEmpty()){
            ados_ap = AP.marked.get(AP.marked.size()-1);
            select_button.setText(ados_ap.essid + " (" + ados_ap.mac + ')');
        }
        if(ssid_file!=null) ((EditText)v.findViewById(R.id.ssid_file)).setText(ssid_file);
        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                bf_switch.setChecked(false);
            }
        };
        ((CheckBox)v.findViewById(R.id.managed)).setChecked(managed);
        ((CheckBox)v.findViewById(R.id.managed)).setOnCheckedChangeListener(listener);
        ((CheckBox)v.findViewById(R.id.adhoc)).setChecked(adhoc);
        ((CheckBox)v.findViewById(R.id.adhoc)).setOnCheckedChangeListener(listener);
        ((CheckBox)v.findViewById(R.id.opn)).setChecked(opn);
        ((CheckBox)v.findViewById(R.id.opn)).setOnCheckedChangeListener(listener);
        ((CheckBox)v.findViewById(R.id.wep)).setChecked(wep);
        ((CheckBox)v.findViewById(R.id.wep)).setOnCheckedChangeListener(listener);
        ((CheckBox)v.findViewById(R.id.tkip)).setChecked(tkip);
        ((CheckBox)v.findViewById(R.id.tkip)).setOnCheckedChangeListener(listener);
        ((CheckBox)v.findViewById(R.id.aes)).setChecked(aes);
        ((CheckBox)v.findViewById(R.id.aes)).setOnCheckedChangeListener(listener);
        refreshDrawer();
    }
    @Override
    public void onPause(){
        super.onPause();
        //Save options
        ssid_file = ((EditText)v.findViewById(R.id.ssid_file)).getText().toString();
        managed = ((CheckBox)v.findViewById(R.id.managed)).isChecked();
        adhoc = ((CheckBox)v.findViewById(R.id.adhoc)).isChecked();
        opn = ((CheckBox)v.findViewById(R.id.opn)).isChecked();
        wep = ((CheckBox)v.findViewById(R.id.wep)).isChecked();
        tkip = ((CheckBox)v.findViewById(R.id.tkip)).isChecked();
        aes = ((CheckBox)v.findViewById(R.id.aes)).isChecked();
    }
}

