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

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.adapter;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getManuf;
import static com.hijacker.MainActivity.isolate;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.startAireplay;
import static com.hijacker.MainActivity.startAireplayWEP;
import static com.hijacker.MainActivity.startAirodump;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.MainActivity.wpa_runnable;
import static com.hijacker.MainActivity.wpa_thread;
import static com.hijacker.MainActivity.wpacheckcont;

class AP {
    static final int WPA=0, WPA2=1, WEP=2, OPN=3, UNKNOWN=4;
    static int wpa=0, wpa2=0, wep=0, opn=0, hidden=0;
    static List <AP>APs = new ArrayList<>();
    boolean isHidden = false;
    int pwr, beacons, data, ivs, ch, id, sec=UNKNOWN;
    String essid, mac, enc, cipher, auth, manuf;
    List <ST>clients = new ArrayList<>();
    Item item;
    AP(String essid, String mac, String enc, String cipher, String auth,
       int pwr, int beacons, int data, int ivs, int ch) {
        this.mac = mac;
        id = APs.size();
        this.manuf = getManuf(this.mac);
        this.update(essid, enc, cipher, auth, pwr, beacons, data, ivs, ch);

        APs.add(this);
    }

    void addClient(ST client){ this.clients.add(client); }
    void update(String essid, String enc, String cipher, String auth,
                              int pwr, int beacons, int data, int ivs, int ch){
        this.essid = essid;
        if(essid.equals("<hidden>") && !isHidden){
            isHidden = true;
            hidden++;
        }
        this.enc = enc;
        this.cipher = cipher;
        this.auth = auth;
        this.pwr = pwr;
        this.ch = ch==-1 ? 0 : ch;      //for hidden networks
        this.beacons = beacons;
        this.data = data;
        this.ivs = ivs;

        if(sec==UNKNOWN){
            switch(this.enc){
                case "WPA":
                    wpa++;
                    sec = WPA;
                    break;
                case "WPA2":
                    wpa2++;
                    sec = WPA2;
                    break;
                case "WEP":
                    wep++;
                    sec = WEP;
                    break;
                case "OPN":
                    opn++;
                    sec = OPN;
                    break;
            }
        }
        String c;
        c = "PWR: " + this.pwr + " | SEC: " + this.enc + " | CH: " + this.ch + " | B:" + this.beacons + " | D:" + this.data;
        if(item!=null) item.update(this.essid, this.mac, c, this.manuf);
        else item = new Item(id, this.essid, this.mac, c, this.manuf, true, this, null);
    }
    static AP getAPByMac(String mac){
        if(mac==null) return null;
        for(int i=APs.size()-1;i>=0;i--){
            if(mac.equals(APs.get(i).mac)) return APs.get(i);
        }
        return null;
    }
    void removeClient(ST st){
        for(int i=clients.size()-1;i>=0;i--){
            if(clients.get(i)==st) clients.remove(i);
        }
    }
    void crack(){
        stop(PROCESS_AIRODUMP);
        stop(PROCESS_AIREPLAY);
        adapter.notifyDataSetChanged();
        if(this.sec == WEP){
            //wep
            if(debug) Log.d("AP", "Cracking WEP");
            startAirodump("--channel " + this.ch + " --bssid " + this.mac + " --ivs -w " + cap_dir + "/wep_ivs");
            if(!this.essid.equals("<hidden>")) startAireplayWEP(this);
            progress.setIndeterminate(true);
        }else if(this.sec == WPA || this.sec == WPA2){
            //wpa/wpa2
            wpacheckcont = false;           //Make sure wpa threads are not running from previous cracking
            wpa_thread.interrupt();
            while(wpa_thread.isAlive())      //Wait for everything to shutdown
            if(debug) Log.d("AP", "Cracking WPA/WPA2");
            startAirodump("--channel " + this.ch + " --bssid " + this.mac + " -w " + cap_dir + "/handshake");
            startAireplay(this.mac);
            wpa_thread = new Thread(wpa_runnable);
            wpa_thread.start();
        }
        if(is_ap==null) isolate(this.mac);
    }
    void crackReaver(FragmentManager fragmentManager){
        ReaverFragment.ap = this;
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragment1, new ReaverFragment());
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();
        fragmentManager.executePendingTransactions();      //Wait for everything to be set up
        ReaverFragment.start_button.performClick();             //Click start to run reaver
    }
    void showInfo(FragmentManager fragmentManager){
        APDialog dialog = new APDialog();
        dialog.info_ap = this;
        dialog.show(fragmentManager, "APDialog");
    }
    void disconnectAll(){
        if(IsolatedFragment.is_ap==null){
            stop(PROCESS_AIRODUMP);
            if(debug) Log.d("AP", "Starting airodump for channel " + this.ch);
            startAirodump("--channel " + this.ch);
        }
        if(debug) {
            Log.d("AP", "Starting aireplay without targets...");
            Log.d("AP", this.mac);
        }
        startAireplay(this.mac);
    }
    public String toString(){
        return mac + '\t' + pwr + '\t' + ch + '\t' + beacons + '\t' + data + '\t' + ivs + '\t' +
                enc + '\t' + auth + '\t' + cipher + '\t' + (isHidden ? "Yes" : "No") + '\t' + essid + '\t' + manuf + '\n';
    }

    static void clear(){
        APs.clear();
        wpa = 0;
        wpa2 = 0;
        wep = 0;
        opn = 0;
        hidden = 0;
    }
}
