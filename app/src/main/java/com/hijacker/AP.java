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
import static com.hijacker.MainActivity.SORT_BEACONS_FRAMES;
import static com.hijacker.MainActivity.SORT_DATA_FRAMES;
import static com.hijacker.MainActivity.SORT_ESSID;
import static com.hijacker.MainActivity.SORT_NOSORT;
import static com.hijacker.MainActivity.SORT_PWR;
import static com.hijacker.MainActivity.adapter;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.completed;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getManuf;
import static com.hijacker.MainActivity.isolate;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.sort;
import static com.hijacker.MainActivity.startAireplay;
import static com.hijacker.MainActivity.startAireplayWEP;
import static com.hijacker.MainActivity.startAirodump;
import static com.hijacker.MainActivity.startAirodumpForAP;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.MainActivity.toSort;
import static com.hijacker.MainActivity.wpa_runnable;
import static com.hijacker.MainActivity.wpa_thread;
import static com.hijacker.MainActivity.wpacheckcont;

class AP {
    static final int WPA=0, WPA2=1, WEP=2, OPN=3, UNKNOWN=4;
    static int wpa=0, wpa2=0, wep=0, opn=0, hidden=0;
    static List<AP> APs = new ArrayList<>();
    static List<AP> marked = new ArrayList<>();
    boolean isHidden = false, isMarked = false;
    int pwr, ch, id, sec=UNKNOWN;
    private int beacons, data, ivs, total_beacons=0, total_data=0, total_ivs=0;
    long lastseen = 0;
    String essid, mac, enc, cipher, auth, manuf;
    List <ST>clients = new ArrayList<>();
    Tile tile;
    AP(String essid, String mac, String enc, String cipher, String auth,
       int pwr, int beacons, int data, int ivs, int ch) {
        this.mac = mac;
        id = APs.size();
        this.manuf = getManuf(this.mac);
        this.update(essid, enc, cipher, auth, pwr, beacons, data, ivs, ch);

        APs.add(this);
        if(sort!=SORT_NOSORT) toSort = true;
    }

    void addClient(ST client){ this.clients.add(client); }
    void update(String essid, String enc, String cipher, String auth,
                              int pwr, int beacons, int data, int ivs, int ch){

        if(!toSort && sort!=SORT_NOSORT){
            switch(sort){
                case SORT_ESSID:
                    if(essid!=null && this.essid!=null){
                        toSort = !this.essid.equals(essid);
                    }
                    break;
                case SORT_BEACONS_FRAMES:
                    toSort = this.beacons!=beacons;
                    break;
                case SORT_DATA_FRAMES:
                    toSort = this.data!=data;
                    break;
                case SORT_PWR:
                    toSort = this.pwr!=pwr;
                    break;
            }
        }

        this.essid = essid;
        if(essid.equals("<hidden>") && !isHidden){
            isHidden = true;
            hidden++;
        }

        if(beacons!=this.beacons || data!=this.data || this.lastseen==0){
            this.lastseen = System.currentTimeMillis();
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
        final String c;
        c = "PWR: " + this.pwr + " | SEC: " + this.enc + " | CH: " + this.ch + " | B:" + this.getBeacons() + " | D:" + this.getData();
        runInHandler(new Runnable(){
            @Override
            public void run(){
                if(tile!=null) tile.update(AP.this.essid, AP.this.mac, c, AP.this.manuf);
                else tile = new Tile(id, AP.this.essid, AP.this.mac, c, AP.this.manuf, true, AP.this, null);
                if(tile.ap==null) tile = null;
                completed = true;
            }
        });
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
            if(debug) Log.d("HIJACKER/AP", "Cracking WEP");
            startAirodumpForAP(this, "--ivs -w " + cap_dir + "/wep_ivs");
            if(!this.essid.equals("<hidden>")) startAireplayWEP(this);
            progress.setIndeterminate(true);
        }else if(this.sec == WPA || this.sec == WPA2){
            //wpa/wpa2
            wpacheckcont = false;           //Make sure wpa threads are not running from previous cracking
            wpa_thread.interrupt();
            while(wpa_thread.isAlive())      //Wait for everything to shutdown
            if(debug) Log.d("HIJACKER/AP", "Cracking WPA/WPA2");
            startAirodumpForAP(this, "-w " + cap_dir + "/handshake");
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
        ft.commitAllowingStateLoss();
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
            if(debug) Log.d("HIJACKER/AP", "Starting airodump for channel " + this.ch);
            startAirodump("--channel " + this.ch);
        }
        if(debug) {
            Log.d("HIJACKER/AP", "Starting aireplay without targets...");
            Log.d("HIJACKER/AP", this.mac);
        }
        startAireplay(this.mac);
    }
    void mark(){
        if(!marked.contains(this)){
            marked.add(this);
        }
        this.isMarked = true;
        Tile.filter();
    }
    void unmark(){
        if(marked.contains(this)){
            marked.remove(this);
        }
        this.isMarked = false;
        Tile.filter();
    }
    public String toString(){
        return mac + '\t' + pwr + '\t' + ch + '\t' + getBeacons() + '\t' + getData() + '\t' + getIvs() + '\t' +
                enc + '\t' + auth + '\t' + cipher + '\t' + (isHidden ? "Yes" : "No") + '\t' + essid + '\t' + manuf + '\n';
    }
    public int getBeacons(){ return total_beacons + beacons; }
    public int getData(){ return total_data + data; }
    public int getIvs(){ return total_ivs + ivs; }
    public static void saveData(){
        AP temp;
        for(int i=0;i<AP.APs.size();i++){
            temp = AP.APs.get(i);
            temp.total_beacons += temp.beacons;
            temp.total_data += temp.data;
            temp.total_ivs += temp.ivs;
            temp.beacons = 0;
            temp.data = 0;
            temp.ivs = 0;
        }
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
