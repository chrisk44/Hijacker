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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.io.File;

import static com.hijacker.MainActivity.FRAGMENT_SETTINGS;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.load;
import static com.hijacker.MainActivity.main;
import static com.hijacker.MainActivity.maincalled;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.tv;

public class SettingsFragment extends PreferenceFragment {
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        String arch = System.getProperty("os.arch");
        if(!arch.equals("armv7l")){
            Preference temp;
            String options[] = {"install_tools", "install_nexmon", "restore_firmware"};
            for(int i=0;i<3;i++){
                temp = findPreference(options[i]);
                temp.setSummary(R.string.incorrect_arch + arch);
                temp.setEnabled(false);
            }
        }

        tv.setMaxHeight(0);
        findPreference("test_tools").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new TestDialog().show(getFragmentManager(), "TestDialog");
                return false;
            }
        });
        findPreference("reset_pref").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new ConfirmResetDialog().show(getFragmentManager(), "ConfirmResetDialog");
                return false;
            }
        });
        findPreference("copy_sample_button").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new CopySampleDialog().show(getFragmentManager(), "CopySampleDialog");
                return false;
            }
        });
        findPreference("install_tools").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new InstallToolsDialog().show(getFragmentManager(), "InstallToolsDialog");
                return false;
            }
        });
        findPreference("install_nexmon").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new InstallFirmwareDialog().show(getFragmentManager(), "InstallFirmwareDialog");
                return false;
            }
        });
        findPreference("restore_firmware").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                File origFirm = new File(path + "/fw_bcmdhd.orig.bin");
                if(!origFirm.exists()){
                    Toast.makeText(getActivity().getApplicationContext(), R.string.no_backup, Toast.LENGTH_SHORT).show();
                }else new RestoreFirmwareDialog().show(getFragmentManager(), "RestoreFragmentDialog");
                return false;
            }
        });
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        load();
        if(!maincalled) main();
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_SETTINGS;
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                load();
            }
        };
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }
    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        super.onPause();
    }
}
