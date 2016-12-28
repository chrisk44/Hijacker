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

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import static com.hijacker.AP.WEP;
import static com.hijacker.AP.WPA;
import static com.hijacker.AP.WPA2;
import static com.hijacker.MainActivity.FRAGMENT_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.copy;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.isolate;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.startAirodump;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.MainActivity.wpa_thread;

public class IsolatedFragment extends Fragment{
    View view;
    static AP is_ap;
    TextView essid, manuf, mac, sec1, numbers, sec2;
    static Thread thread;
    static Runnable runnable;
    static boolean cont = false;
    static int exit_on;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        view = inflater.inflate(R.layout.isolated_fragment, container, false);

        runnable = new Runnable(){
            @Override
            public void run(){
                cont = true;
                while(cont){
                    try{
                        Thread.sleep(1000);
                        refresh.obtainMessage().sendToTarget();
                    }catch(InterruptedException ignored){}
                }
            }
        };
        thread = new Thread(runnable);

        essid = (TextView)view.findViewById(R.id.essid);
        manuf = (TextView)view.findViewById(R.id.manuf);
        mac = (TextView)view.findViewById(R.id.mac);
        sec1 = (TextView)view.findViewById(R.id.sec1);
        numbers = (TextView)view.findViewById(R.id.numbers);
        sec2 = (TextView)view.findViewById(R.id.sec2);

        ListView listview = (ListView)view.findViewById(R.id.listview);
        listview.setAdapter(MainActivity.adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View v, int i, long l){
                final Item clicked = Item.items.get(i);

                //ST
                PopupMenu popup = new PopupMenu(getActivity(), v);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                popup.getMenu().add(0, 0, 0, "Info");
                popup.getMenu().add(0, 1, 1, "Copy MAC");
                if(clicked.st.bssid!=null){
                    popup.getMenu().add(0, 2, 2, "Disconnect");
                    popup.getMenu().add(0, 3, 3, "Copy disconnect command");
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        if(debug) Log.d("HIJACKER/MyListFragment", "Clicked " + item.getItemId() + " for st");
                        switch(item.getItemId()) {
                            case 0:
                                //Info
                                clicked.st.showInfo(getFragmentManager());
                                break;
                            case 1:
                                //copy to clipboard
                                copy(clicked.st.mac, v);
                                break;
                            case 2:
                                //Disconnect this
                                clicked.st.disconnect();
                                break;
                            case 3:
                                //copy disconnect command to clipboard
                                String str = prefix + " " + aireplay_dir + " --ignore-negative-one --deauth 0 -a " + clicked.st.bssid + " -c " + clicked.st.mac + " " + iface;
                                copy(str, v);
                                break;
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        return view;
    }
    public Handler refresh = new Handler(){
        public void handleMessage(Message msg){
            if(cont && is_ap !=null){
                essid.setText(is_ap.essid);
                manuf.setText(is_ap.manuf);
                mac.setText(is_ap.mac);
                sec1.setText("Enc: " + is_ap.enc + " | Auth: " + is_ap.auth + " | Cipher: " + is_ap.cipher);
                numbers.setText("B: " + is_ap.beacons + " | D: " + is_ap.data + " | #s: " + is_ap.ivs);
                sec2.setText("PWR: " + is_ap.pwr + "Channel: " + is_ap.ch);
            }
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_AIRODUMP;
        thread = new Thread(runnable);
        thread.start();
        ((Button)view.findViewById(R.id.crack)).setText(wpa_thread.isAlive() ? R.string.stop : R.string.crack);
        view.findViewById(R.id.crack).setEnabled(is_ap.sec==WPA || is_ap.sec==WPA2 || is_ap.sec==WEP);
        ((Button)view.findViewById(R.id.dos)).setText(MDKFragment.ados ? R.string.stop : R.string.dos);
        refresh.obtainMessage().sendToTarget();
    }
    @Override
    public void onPause(){
        super.onPause();
        cont = false;
        thread.interrupt();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        cont = false;
        if(getFragmentManager().getBackStackEntryCount()==exit_on){
            isolate(null);
            stop(PROCESS_AIRODUMP);
            stop(PROCESS_AIREPLAY);
            startAirodump(null);
        }
    }
}
