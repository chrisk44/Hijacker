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

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import static com.hijacker.MainActivity.FRAGMENT_SETTINGS;
import static com.hijacker.MainActivity.arch;
import static com.hijacker.MainActivity.checkForUpdate;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.pref_edit;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.versionName;
import static com.hijacker.MainActivity.watchdog;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.load;

public class SettingsFragment extends PreferenceFragment {
    static boolean allow_prefix = false;
    int versionClicks = 0;
    long lastVersionClick = 0;
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        if(!arch.equals("armv7l") && !arch.equals("aarch64")){
            Preference pref = findPreference("install_nexmon");
            pref.setSummary(getString(R.string.incorrect_arch) + ' ' + arch);
            pref.setEnabled(false);

            findPreference("prefix").setEnabled(true);
        }
        if(allow_prefix) findPreference("prefix").setEnabled(true);

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
        findPreference("send_feedback").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                new FeedbackDialog().show(getFragmentManager(), "FeedbackDialog");
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
        findPreference("update_on_startup").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue){
                if((boolean)newValue){
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            Looper.prepare();
                            checkForUpdate(SettingsFragment.this.getActivity(), true);
                        }
                    }).start();
                }
                return true;
            }
        });
        findPreference("version").setSummary(versionName);
        /*
         * Disable DevOptionsFragment because people are sending bug reports
         * after clicking the "CRASH_THE_APP" button.

        findPreference("version").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                long time = System.currentTimeMillis();
                if(time - lastVersionClick < 1000){
                    if(versionClicks < 4){
                        versionClicks++;
                    }else{
                        versionClicks = 0;
                        FragmentTransaction ft = mFragmentManager.beginTransaction();
                        ft.replace(R.id.fragment1, new DevOptionsFragment());
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        ft.addToBackStack(null);
                        ft.commitAllowingStateLoss();
                    }
                }else versionClicks = 1;
                lastVersionClick = time;
                return false;
            }
        });
        */
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
                boolean running = ((MainActivity)getActivity()).watchdogTask.isRunning();
                if(watchdog && !running){
                    //Turned off
                    ((MainActivity)getActivity()).watchdogTask = new WatchdogTask(getActivity());
                    ((MainActivity)getActivity()).watchdogTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }else if(!watchdog && running){
                    //Turned on
                    ((MainActivity)getActivity()).watchdogTask.cancel(true);
                }
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
