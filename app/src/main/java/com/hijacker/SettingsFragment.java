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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.io.File;

import static com.hijacker.MainActivity.FRAGMENT_SETTINGS;
import static com.hijacker.MainActivity.arch;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.version;
import static com.hijacker.MainActivity.watchdog;
import static com.hijacker.MainActivity.watchdog_runnable;
import static com.hijacker.MainActivity.watchdog_thread;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.load;
import static com.hijacker.MainActivity.path;

public class SettingsFragment extends PreferenceFragment {
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        if(!arch.equals("armv7l")){
            Preference temp;
            String options[] = {"install_tools", "install_nexmon", "restore_firmware"};
            for(int i=0;i<3;i++){
                temp = findPreference(options[i]);
                temp.setSummary(R.string.incorrect_arch + arch);
                temp.setEnabled(false);
            }
        }

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
        File origFirm = new File(path + "/fw_bcmdhd.orig.bin");
        if(!origFirm.exists()){
            findPreference("restore_firmware").setEnabled(false);
        }else{
            findPreference("restore_firmware").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference){
                    new RestoreFirmwareDialog().show(getFragmentManager(), "RestoreFragmentDialog");
                    return false;
                }
            });
        }
        findPreference("send_feedback").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                Intent intent = new Intent (Intent.ACTION_SEND);
                intent.setType("plain/text");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"kiriakopoulos44@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Hijacker feedback");
                startActivity(intent);
                return false;
            }
        });
        findPreference("github").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://github.com/chrisk44/Hijacker"));
                startActivity(intent);
                return false;
            }
        });
        findPreference("version").setSummary(version);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        load();
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_SETTINGS;
        refreshDrawer();
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                load();
                if(watchdog && !watchdog_thread.isAlive()){
                    watchdog_thread = new Thread(watchdog_runnable);
                    watchdog_thread.start();
                }
                if(!watchdog && watchdog_thread.isAlive()) watchdog_thread.interrupt();
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
