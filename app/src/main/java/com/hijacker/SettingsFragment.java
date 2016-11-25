package com.hijacker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hijacker.MainActivity.inSettings;
import static com.hijacker.MainActivity.load;
import static com.hijacker.MainActivity.main;
import static com.hijacker.MainActivity.maincalled;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.pref;
import static com.hijacker.MainActivity.pref_edit;
import static com.hijacker.MainActivity.shell;
import static com.hijacker.MainActivity.shell3_in;
import static com.hijacker.MainActivity.su_thread;
import static com.hijacker.MainActivity.tv;

public class SettingsFragment extends PreferenceFragment {
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inSettings = true;
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
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
                if(shell==null){
                    su_thread.start();
                    try{
                        //Wait for su shells to spawn
                        su_thread.join();
                    }catch(InterruptedException ignored){}
                }
                shell3_in.print("cp -n /vendor/firmware/fw_bcmdhd.bin /sdcard/fw_bcmdhd.bin.original\n");
                shell3_in.flush();
                extract("fw_bcmdhd.bin", "/vendor/firmware");
                extract("nexutil", "/su/xbin");
                Toast.makeText(getActivity().getApplicationContext(), "Installed firmware and utility", Toast.LENGTH_LONG).show();
                return false;
            }
        });
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        inSettings = false;
        load();
        if(!maincalled) main();
    }
    @Override
    public void onResume() {
        super.onResume();
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
    void extract(String filename, String dest){
        File f = new File(path, filename);      //no permissions to write at dest
        dest = dest + '/' + filename;
        if(!f.exists()){
            try{
                InputStream in = getResources().getAssets().open(filename);
                FileOutputStream out = new FileOutputStream(f);
                byte[] buf = new byte[1024];
                int len;
                while((len = in.read(buf))>0){
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                shell3_in.print("mv " + path + '/' + filename + " " + dest + '\n');
                shell3_in.print("chmod 755 " + dest + '\n');
                shell3_in.flush();
            }catch(IOException e){
                Log.e("FileProvider", "Exception copying from assets", e);
            }
        }
    }
}
