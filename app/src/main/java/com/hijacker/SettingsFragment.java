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
import static com.hijacker.MainActivity.isArchValid;
import static com.hijacker.MainActivity.loadPreferences;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.pref_edit;
import static com.hijacker.MainActivity.versionName;
import static com.hijacker.MainActivity.watchdog;
import static com.hijacker.MainActivity.currentFragment;

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

        if(!isArchValid()){
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
                CustomDialog dialog = new CustomDialog();
                dialog.setTitle(getString(R.string.reset_dialog_title));
                dialog.setMessage(getString(R.string.reset_dialog_message));
                dialog.setPositiveButton(getString(R.string.yes), new Runnable(){
                    @Override
                    public void run(){
                        pref_edit.putString("iface", getString(R.string.iface));
                        pref_edit.putString("prefix", getString(R.string.prefix));
                        pref_edit.putString("enable_monMode", getString(R.string.enable_monMode));
                        pref_edit.putString("disable_monMode", getString(R.string.disable_monMode));
                        pref_edit.putBoolean("enable_on_airodump", Boolean.parseBoolean(getString(R.string.enable_on_airodump)));
                        pref_edit.putString("deauthWait", getString(R.string.deauthWait));
                        pref_edit.putBoolean("show_notif", Boolean.parseBoolean(getString(R.string.show_notif)));
                        pref_edit.putBoolean("show_details", Boolean.parseBoolean(getString(R.string.show_details)));
                        pref_edit.putBoolean("airOnStartup", Boolean.parseBoolean(getString(R.string.airOnStartup)));
                        pref_edit.putBoolean("debug", Boolean.parseBoolean(getString(R.string.debug)));
                        pref_edit.putBoolean("delete_extra", Boolean.parseBoolean(getString(R.string.delete_extra)));
                        pref_edit.putBoolean("always_cap", Boolean.parseBoolean(getString(R.string.always_cap)));
                        pref_edit.putString("chroot_dir", getString(R.string.chroot_dir));
                        pref_edit.putBoolean("monstart", Boolean.parseBoolean(getString(R.string.monstart)));
                        pref_edit.putString("custom_chroot_cmd", "");
                        pref_edit.putBoolean("cont_on_fail", Boolean.parseBoolean(getString(R.string.cont_on_fail)));
                        pref_edit.putBoolean("watchdog", Boolean.parseBoolean(getString(R.string.watchdog)));
                        pref_edit.putBoolean("target_deauth", Boolean.parseBoolean(getString(R.string.target_deauth)));
                        pref_edit.putBoolean("update_on_startup", Boolean.parseBoolean(getString(R.string.auto_update)));
                        pref_edit.apply();
                        loadPreferences();
                    }
                });
                dialog.setNegativeButton(getString(R.string.cancel), null);
                dialog.show(getFragmentManager(), "CustomDialog for reset confirmation");
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
                        pref_edit.apply();
                        loadPreferences();
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
                            ((MainActivity)getActivity()).checkForUpdate(true);
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
        final MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.refreshDrawer();
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                loadPreferences();
                boolean running = mainActivity.watchdogTask.isRunning();
                if(watchdog && !running){
                    //Turned off
                    mainActivity.watchdogTask = new WatchdogTask(getActivity());
                    mainActivity.watchdogTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }else if(!watchdog && running){
                    //Turned on
                    mainActivity.watchdogTask.cancel(true);
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
