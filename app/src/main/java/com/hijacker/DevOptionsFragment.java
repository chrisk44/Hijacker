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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import static com.hijacker.AP.getAPByMac;
import static com.hijacker.MainActivity.FRAGMENT_SETTINGS;
import static com.hijacker.MainActivity.NETHUNTER_BOOTKALI_BASH;
import static com.hijacker.MainActivity.bootkali_init_bin;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.ReaverFragment.get_chroot_env;

public class DevOptionsFragment extends PreferenceFragment{
    View fragmentView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dev_options);

        findPreference("causeNPE").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                getAPByMac(null).crack();
                return false;
            }
        });
        findPreference("testChroot").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    if(bootkali_init_bin.equals(NETHUNTER_BOOTKALI_BASH)){
                        //Not in nethunter, need to initialize the chroot environment
                        Log.d("TESTESTEST", "Initializing chroot environment");
                        Runtime.getRuntime().exec("su -c " + bootkali_init_bin);       //Make sure kali has booted
                    }else{
                        Log.d("TESTESTEST", "No need to initialize chroot environment");
                    }

                    String cmd = "su -c chroot " + MainActivity.chroot_dir + " /bin/bash -c \"" + get_chroot_env(getActivity()) + "echo asd; echo asd; if [[ -r /dev/urandom ]]; then echo Success; else echo Fail; fi; \"; exit";
                    Log.d("TESTESTEST", "CMD: " + cmd);

                    ProcessBuilder pb = new ProcessBuilder("su");
                    pb.redirectErrorStream(true);
                    Process dc = pb.start();
                    BufferedReader out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                    PrintWriter in = new PrintWriter(dc.getOutputStream());
                    in.print(cmd + "\nexit\n");
                    in.flush();

                    String buffer = out.readLine();
                    while(buffer!=null){
                        Log.d("TESTESTEST Output", buffer);
                        buffer = out.readLine();
                    }
                    Log.d("TESTESTEST", "Finished reading output");
                }catch(Exception e){
                    e.printStackTrace();
                }
                return false;
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_SETTINGS;
        refreshDrawer();
        fragmentView = getView();
    }
}
