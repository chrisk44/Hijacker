package com.hijacker;

import java.util.ArrayList;
import java.util.List;

import static com.hijacker.MainActivity.getManuf;
import static com.hijacker.MainActivity.is_ap;

class ST {
    static List <ST>STs = new ArrayList<>();
    static String paired, not_connected;
    int pwr, lost, frames, id;
    boolean added_as_client = false;
    Item item;
    String mac, bssid, manuf;
    ST(String mac, String bssid, int pwr, int lost, int frames){
        this.mac = mac;
        this.id = STs.size();
        this.manuf = getManuf(this.mac);
        this.update(bssid, pwr, lost, frames);
        STs.add(this);
    }
    void disconnect(){
        if(is_ap==null){
            //need to switch channel only if there is no isolated ap
            MainActivity.stop(0);
            MainActivity.startAirodump("--channel " + AP.getAPByMac(this.bssid).ch);
        }
        MainActivity.stop(1);
        MainActivity.startAireplay(this.bssid, this.mac);
    }
    void update(String bssid, int pwr, int lost, int frames){
        if(bssid=="na") bssid=null;

        if(added_as_client && bssid==null){
            added_as_client = false;
            AP.getAPByMac(this.bssid).removeClient(this);
        }else if(!added_as_client && bssid!=null){
            AP temp = AP.getAPByMac(bssid);
            if (temp != null){
                this.bssid = bssid;
                temp.addClient(this);
                added_as_client = true;
            }
        }

        this.bssid = bssid;
        this.pwr = pwr;
        this.lost = lost;
        this.frames = frames;

        String b, c;
        if (bssid != null){
            if(AP.getAPByMac(bssid) != null) b = paired + bssid + " (" + AP.getAPByMac(bssid).essid + ")";
            else b = paired + bssid;
        } else b = not_connected;
        c = "PWR: " + this.pwr + " | Frames: " + this.frames;
        if(item!=null) item.update(this.mac, b, c, this.manuf);
        else item = new Item(AP.APs.size() + id, this.mac, b, c, this.manuf, false, null, this);
    }
    static ST getSTByMac(String mac){
        for(int i=STs.size()-1;i>=0;i--){
            if(mac.equals(STs.get(i).mac)){
                return STs.get(i);
            }
        }
        return null;
    }
    static void clear(){ STs.clear(); }
}
