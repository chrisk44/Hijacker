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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import static com.hijacker.AP.getAPByMac;
import static com.hijacker.MainActivity.FRAGMENT_SETTINGS;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.currentFragment;

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
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_SETTINGS;
        refreshDrawer();
        fragmentView = getView();
    }
}
