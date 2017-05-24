package com.hijacker;

/*
    Copyright (C) 2017  Christos Kyriakopoylos

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
import android.os.Looper;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import static com.hijacker.AP.getAPByMac;
import static com.hijacker.MainActivity.AUTH_KEY;
import static com.hijacker.MainActivity.FRAGMENT_SETTINGS;
import static com.hijacker.MainActivity.PORT;
import static com.hijacker.MainActivity.REQ_EXIT;
import static com.hijacker.MainActivity.connect;
import static com.hijacker.MainActivity.deviceID;
import static com.hijacker.MainActivity.pref_edit;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.runInHandler;

public class DevOptionsFragment extends PreferenceFragment{
    static final String AUTH_KEY_TEST = "key-that-will-be-changed-in-the-release-build_this-is-for-testing";
    static final String backupKey = AUTH_KEY;
    Preference causeNPE, connect, disconnect, resetID;
    SwitchPreference useTestKey;
    EditTextPreference port;
    Socket testSocket = null;
    View fragmentView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dev_options);

        causeNPE = findPreference("causeNPE");
        connect = findPreference("connect");
        disconnect = findPreference("disconnect");
        useTestKey = (SwitchPreference)findPreference("useTestKey");
        port = (EditTextPreference)findPreference("port");
        resetID = findPreference("resetID");

        connect.setEnabled(testSocket==null);
        disconnect.setEnabled(testSocket!=null);
        useTestKey.setChecked(backupKey.equals(AUTH_KEY_TEST));

        causeNPE.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                getAPByMac(null).crack();
                return false;
            }
        });
        connect.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                if(testSocket==null){
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            Looper.prepare();
                            testSocket = connect();
                            if(testSocket!=null){
                                runInHandler(new Runnable(){
                                    @Override
                                    public void run(){
                                        disconnect.setEnabled(true);
                                        connect.setEnabled(false);
                                    }
                                });
                                Snackbar.make(fragmentView, "Socket opened", Snackbar.LENGTH_SHORT).show();
                            }else{
                                Snackbar.make(fragmentView, "Socket is null", Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }).start();
                }
                return false;
            }
        });
        disconnect.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                if(testSocket!=null){
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            Looper.prepare();
                            try{
                                PrintWriter in = new PrintWriter(testSocket.getOutputStream());
                                in.print(REQ_EXIT + '\n');
                                in.flush();

                                in.close();
                                testSocket.close();
                                runInHandler(new Runnable(){
                                    @Override
                                    public void run(){
                                        connect.setEnabled(true);
                                        disconnect.setEnabled(false);
                                    }
                                });
                                testSocket = null;
                                Snackbar.make(fragmentView, "Socket closed", Snackbar.LENGTH_SHORT).show();
                            }catch(IOException e){
                                Log.e("HIJACKER/devOptions", e.toString());
                                Snackbar.make(fragmentView, "IOException", Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }).start();
                }

                return false;
            }
        });
        useTestKey.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue){
                if((boolean)newValue){
                    //Change to AUTH_KEY_TEST
                    AUTH_KEY = AUTH_KEY_TEST;
                    Snackbar.make(fragmentView, "Using test key", Snackbar.LENGTH_SHORT).show();
                }else{
                    //Change to AUTH_KEY
                    AUTH_KEY = backupKey;
                    Snackbar.make(fragmentView, "Using normal key", Snackbar.LENGTH_SHORT).show();
                }
                Log.d("HIJACKER/devOptions", "AUTH_KEY.equals(backupKey) = " + AUTH_KEY.equals(AUTH_KEY_TEST));
                return true;
            }
        });
        port.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue){
                PORT = Integer.parseInt((String)newValue);
                Snackbar.make(fragmentView, "Port is now " + PORT, Snackbar.LENGTH_SHORT).show();
                return false;
            }
        });
        resetID.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference){
                pref_edit.putLong("deviceID", -1);
                pref_edit.commit();

                deviceID = -1;

                Snackbar.make(fragmentView, "deviceID is now -1", Snackbar.LENGTH_SHORT).show();
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
