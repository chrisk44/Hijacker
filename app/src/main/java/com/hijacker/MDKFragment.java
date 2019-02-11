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

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Switch;

import static com.hijacker.MainActivity.FRAGMENT_MDK;
import static com.hijacker.MainActivity.PROCESS_MDK_BF;
import static com.hijacker.MainActivity.PROCESS_MDK_DOS;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.startAdos;
import static com.hijacker.MainActivity.startBeaconFlooding;
import static com.hijacker.MainActivity.stop;

public class MDKFragment extends Fragment{
    View fragmentView;
    static AP ados_ap=null;
    static Switch bf_switch, ados_switch;
    EditText ssidView;
    CheckBox managed_cb, adhoc_cb, opn_cb, wep_cb, tkip_cb, aes_cb;
    Button select_button;
    static String custom_mac=null, ssid_file=null;
    static boolean managed=true, adhoc=true, opn=true, wep=true, tkip=true, aes=true;
    static boolean bf=false, ados=false;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        setRetainInstance(true);
        fragmentView = inflater.inflate(R.layout.mdk_fragment, container, false);

        ssidView = fragmentView.findViewById(R.id.ssid_file);
        managed_cb = fragmentView.findViewById(R.id.managed);
        adhoc_cb = fragmentView.findViewById(R.id.adhoc);
        opn_cb = fragmentView.findViewById(R.id.opn);
        wep_cb = fragmentView.findViewById(R.id.wep);
        tkip_cb = fragmentView.findViewById(R.id.tkip);
        aes_cb = fragmentView.findViewById(R.id.aes);
        bf_switch = fragmentView.findViewById(R.id.bf_switch);
        ados_switch = fragmentView.findViewById(R.id.ados_switch);
        select_button = fragmentView.findViewById(R.id.select_ap_ados);

        fragmentView.findViewById(R.id.ssid_file_fe_btn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setToSelect(FileExplorerDialog.SELECT_EXISTING_FILE);
                dialog.setStartingDir(new RootFile(Environment.getExternalStorageDirectory().toString()));
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        ssidView.setText(dialog.result.getAbsolutePath());
                        ssidView.setError(null);
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
            }
        });

        bf_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                onBfSwitch(b);
            }
        });
        ados_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                onDosSwitch(b);
            }
        });
        select_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                onSelectClick(view);
            }
        });

        return fragmentView;
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_MDK;
        //Restore options
        bf_switch.setChecked(bf);
        ados_switch.setChecked(ados);
        if(custom_mac!=null) select_button.setText(custom_mac);
        else if(ados_ap!=null) select_button.setText(ados_ap.toString());
        else if(!AP.marked.isEmpty()){
            ados_ap = AP.marked.get(AP.marked.size()-1);
            select_button.setText(ados_ap.toString());
        }
        if(ssid_file!=null) ssidView.setText(ssid_file);
        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                bf_switch.setChecked(false);
            }
        };
        managed_cb.setChecked(managed);
        managed_cb.setOnCheckedChangeListener(listener);
        adhoc_cb.setChecked(adhoc);
        adhoc_cb.setOnCheckedChangeListener(listener);
        opn_cb.setChecked(opn);
        opn_cb.setOnCheckedChangeListener(listener);
        wep_cb.setChecked(wep);
        wep_cb.setOnCheckedChangeListener(listener);
        tkip_cb.setChecked(tkip);
        tkip_cb.setOnCheckedChangeListener(listener);
        aes_cb.setChecked(aes);
        aes_cb.setOnCheckedChangeListener(listener);
        refreshDrawer();
    }
    @Override
    public void onPause(){
        super.onPause();
        //Save options
        ssid_file = ssidView.getText().toString();
        managed = managed_cb.isChecked();
        adhoc = adhoc_cb.isChecked();
        opn = opn_cb.isChecked();
        wep = wep_cb.isChecked();
        tkip = tkip_cb.isChecked();
        aes = aes_cb.isChecked();
    }

    void onSelectClick(View view){
        PopupMenu popup = new PopupMenu(getActivity(), view);

        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        int i = 0;
        for(AP ap : AP.APs){
            popup.getMenu().add(0, i, i, ap.toString());
            i++;
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
                                stop(PROCESS_MDK_DOS);
                            }
                        });
                    }
                    select_button.setText(ados_ap.toString());
                }else{
                    //Clcked custom
                    final EditTextDialog dialog = new EditTextDialog();
                    dialog.setTitle(getString(R.string.custom_ap_title));
                    dialog.setHint(getString(R.string.mac_address));
                    dialog.setRunnable(new Runnable(){
                        @Override
                        public void run(){
                            ados_ap = null;
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
    void onBfSwitch(boolean b){
        if(b){
            ssidView.setError(null);
            String ssid_file = ssidView.getText().toString();
            managed = managed_cb.isChecked();
            adhoc = adhoc_cb.isChecked();
            opn = opn_cb.isChecked();
            wep = wep_cb.isChecked();
            tkip = tkip_cb.isChecked();
            aes = aes_cb.isChecked();
            String args = "";
            if(!managed && !adhoc){
                Snackbar.make(fragmentView, getString(R.string.select_type), Snackbar.LENGTH_LONG).show();
                bf_switch.setChecked(false);
                return;
            }
            if(!(managed && adhoc)){
                if(managed) args += " -t 0";
                if(adhoc) args += " -t 1";
            }
            if(!(opn || wep || tkip || aes)){
                Snackbar.make(fragmentView, getString(R.string.select_enc), Snackbar.LENGTH_LONG).show();
                bf_switch.setChecked(false);
                return;
            }
            if(!(ssid_file.equals("") || ssid_file.startsWith("/"))){
                ssidView.setError(getString(R.string.filename_invalid));
                ssidView.requestFocus();
                bf_switch.setChecked(false);
                return;
            }
            args += " -w ";
            if(opn) args += 'n';
            if(wep) args += 'w';
            if(tkip) args += 't';
            if(aes) args += 'a';
            if(!ssid_file.equals("")){
                RootFile ssidRootFile = new RootFile(ssid_file);
                if(!ssidRootFile.isFile()){
                    ssidView.setError(getString(R.string.not_file_or_exists));
                    ssidView.requestFocus();
                    bf_switch.setChecked(false);
                    return;
                }else{
                    args += " -f " + ssid_file;
                }
            }
            startBeaconFlooding(args);
        }else{
            bf = false;
            stop(PROCESS_MDK_BF);
        }
    }
    void onDosSwitch(boolean b){
        if(b){
            startAdos(ados_ap==null ? custom_mac : ados_ap.mac);
        }else{
            ados = false;
            stop(PROCESS_MDK_DOS);
        }
    }
}

