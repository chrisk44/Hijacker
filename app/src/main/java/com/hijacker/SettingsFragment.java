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
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.io.File;

import static com.hijacker.MainActivity.FRAGMENT_SETTINGS;
import static com.hijacker.MainActivity.arch;
import static com.hijacker.MainActivity.firm_backup_file;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.pref_edit;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.version;
import static com.hijacker.MainActivity.watchdog;
import static com.hijacker.MainActivity.watchdog_runnable;
import static com.hijacker.MainActivity.watchdog_thread;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.load;

public class SettingsFragment extends PreferenceFragment {
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        if(!arch.equals("armv7l")){
            Preference temp;
            String toDisable[] = {"install_nexmon", "restore_firmware"};
            for(String option : toDisable){
                temp = findPreference(option);
                temp.setSummary(R.string.incorrect_arch + ' ' + arch);
                temp.setEnabled(false);
            }
            if(!arch.equals("aarch64")) findPreference("prefix").setEnabled(true);
        }

        findPreference("test_tools").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new TestDialog().show(mFragmentManager, "TestDialog");
                return false;
            }
        });
        findPreference("reset_pref").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new ConfirmResetDialog().show(mFragmentManager, "ConfirmResetDialog");
                return false;
            }
        });
        findPreference("copy_sample_button").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new CopySampleDialog().show(mFragmentManager, "CopySampleDialog");
                return false;
            }
        });
        findPreference("install_nexmon").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new InstallFirmwareDialog().show(mFragmentManager, "InstallFirmwareDialog");
                return false;
            }
        });
        File origFirm = new File(firm_backup_file);
        if(!origFirm.exists()){
            findPreference("restore_firmware").setEnabled(false);
        }else{
            findPreference("restore_firmware").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference){
                    new RestoreFirmwareDialog().show(mFragmentManager, "RestoreFragmentDialog");
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
        findPreference("cap_dir").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setToSelect(FileExplorerDialog.SELECT_DIR);
                dialog.setStartingDir(new RootFile(Environment.getExternalStorageDirectory().toString()));
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        pref_edit.putString("cap_dir", dialog.result.getAbsolutePath());
                        pref_edit.commit();
                        load();
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
                return false;
            }
        });
        findPreference("chroot_dir").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setToSelect(FileExplorerDialog.SELECT_DIR);
                dialog.setStartingDir(new RootFile("/data/local/"));
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        pref_edit.putString("chroot_dir", dialog.result.getAbsolutePath());
                        pref_edit.commit();
                        load();
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
                return false;
            }
        });
        findPreference("version").setSummary(version);
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
