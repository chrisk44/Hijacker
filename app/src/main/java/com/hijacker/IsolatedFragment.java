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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import static com.hijacker.AP.WEP;
import static com.hijacker.AP.WPA;
import static com.hijacker.AP.WPA2;
import static com.hijacker.MainActivity.FRAGMENT_AIRODUMP;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.isolate;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.menu;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.wpa_thread;

public class IsolatedFragment extends Fragment{
    static AP is_ap;
    private Thread thread;
    private Runnable runnable;
    private boolean cont = false;
    static int exit_on;
    View fragmentView;
    TextView essid, manuf, mac, sec1, numbers, sec2;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        fragmentView = inflater.inflate(R.layout.isolated_fragment, container, false);

        runnable = new Runnable(){
            @Override
            public void run(){
                cont = true;
                try{
                    while(cont){
                        Thread.sleep(1000);
                        runInHandler(refreshRunnable);
                    }
                }catch(InterruptedException ignored){}
            }
        };
        thread = new Thread(runnable);

        essid = fragmentView.findViewById(R.id.essid);
        manuf = fragmentView.findViewById(R.id.manuf);
        mac = fragmentView.findViewById(R.id.mac);
        sec1 = fragmentView.findViewById(R.id.sec1);
        numbers = fragmentView.findViewById(R.id.numbers);
        sec2 = fragmentView.findViewById(R.id.sec2);

        ListView listview = fragmentView.findViewById(R.id.listview);
        listview.setAdapter(MainActivity.adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View v, int i, long l){
                Tile.tiles.get(i).device.getPopupMenu((MainActivity)getActivity(), v).show();
            }
        });

        return fragmentView;
    }
    Runnable refreshRunnable = new Runnable(){
        @Override
        public void run(){
            if(cont && is_ap !=null){
                essid.setText(is_ap.essid);
                manuf.setText(is_ap.manuf);
                mac.setText(is_ap.mac);
                sec1.setText("Enc: " + is_ap.enc + " | Auth: " + is_ap.auth + " | Cipher: " + is_ap.cipher);
                numbers.setText("B: " + is_ap.getBeacons() + " | D: " + is_ap.getData() + " | #s: " + is_ap.getIvs());
                sec2.setText("PWR: " + is_ap.pwr + " | Channel: " + is_ap.ch);
            }
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_AIRODUMP;
        refreshDrawer();
        thread = new Thread(runnable);
        thread.start();
        ((Button)fragmentView.findViewById(R.id.crack)).setText(wpa_thread.isAlive() ? R.string.stop : R.string.crack);
        fragmentView.findViewById(R.id.crack).setEnabled(is_ap.sec==WPA || is_ap.sec==WPA2 || is_ap.sec==WEP);
        ((Button)fragmentView.findViewById(R.id.dos)).setText(MDKFragment.ados ? R.string.stop : R.string.dos);
        runInHandler(refreshRunnable);
        menu.findItem(R.id.reset).setVisible(false);
    }
    @Override
    public void onPause(){
        super.onPause();
        cont = false;
        thread.interrupt();
        menu.findItem(R.id.reset).setVisible(true);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mFragmentManager.getBackStackEntryCount()==exit_on){
            isolate(null);
            Airodump.startClean();
        }
    }
}
