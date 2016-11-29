package com.hijacker;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MainActivity.adapter;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getManuf;
import static com.hijacker.MainActivity.isolate;
import static com.hijacker.MainActivity.startAireplay;
import static com.hijacker.MainActivity.startAireplayWEP;
import static com.hijacker.MainActivity.startAirodump;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.MainActivity.wpa_subthread;
import static com.hijacker.MainActivity.wpa_thread;
import static com.hijacker.MainActivity.wpacheckcont;

class AP {
    static final int WPA=0, WPA2=1, WEP=2, OPN=3, UNKNOWN=4;
    static int wpa=0, wpa2=0, wep=0, opn=0;
    static List <AP>APs = new ArrayList<>();
    boolean isHidden = false;
    int pwr, beacons, data, ivs, ch, id, sec;
    String essid, mac, enc, cipher, auth, manuf;
    List <ST>clients = new ArrayList<>();
    Item item;
    AP(String essid, String mac, String enc, String cipher, String auth,
       int pwr, int beacons, int data, int ivs, int ch) {
        this.mac = mac;
        id = APs.size();
        this.manuf = getManuf(this.mac);
        this.update(essid, enc, cipher, auth, pwr, beacons, data, ivs, ch);

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
            default:
                sec = UNKNOWN;
        }
        APs.add(this);
    }

    void addClient(ST client){ this.clients.add(client); }
    void update(String essid, String enc, String cipher, String auth,
                              int pwr, int beacons, int data, int ivs, int ch){
        this.essid = essid;
        if(essid.equals("<hidden>")) isHidden = true;
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
        this.beacons=0;
        this.data=0;
        this.ivs=0;
        stop(0);
        stop(1);
        adapter.notifyDataSetChanged();
        if(this.sec == WEP){
            //wep
            if(debug) Log.d("AP", "Cracking WEP");
            startAirodump("--channel " + this.ch + " --bssid " + this.mac + " --ivs -w " + cap_dir + "/wep.cap");
            startAireplayWEP(this.mac);
        }else if(this.sec == WPA || this.sec == WPA2){
            //wpa/wpa2
            wpacheckcont = false;
            wpa_thread.interrupt();
            MainActivity.tv.setText("");
            while(wpa_thread.isAlive() || wpa_subthread.isAlive())
            if(debug) Log.d("AP", "Cracking WPA/WPA2");
            startAirodump("--channel " + this.ch + " --bssid " + this.mac + " -w " + cap_dir + "/wpa.cap");
            startAireplay(this.mac);
            wpa_thread.start();
        }
        if(is_ap==null) isolate(this.mac);
    }
    static void clear(){
        APs.clear();
        wpa = 0;
        wpa2 = 0;
        wep = 0;
        opn = 0;
    }
}
