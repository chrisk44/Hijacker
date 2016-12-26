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

import java.util.ArrayList;
import java.util.List;

import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.getManuf;
import static com.hijacker.MainActivity.startAireplay;
import static com.hijacker.MainActivity.startAirodump;
import static com.hijacker.MainActivity.stop;

class ST {
    static List <ST>STs = new ArrayList<>();
    static String paired, not_connected;
    static int connected=0;
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
            stop(PROCESS_AIRODUMP);
            startAirodump("--channel " + AP.getAPByMac(this.bssid).ch);
        }
        stop(PROCESS_AIREPLAY);
        startAireplay(this.bssid, this.mac);
    }
    void update(String bssid, int pwr, int lost, int frames){
        if(bssid=="na") bssid=null;

        if(added_as_client && bssid==null){
            connected--;
            added_as_client = false;
            AP.getAPByMac(this.bssid).removeClient(this);
        }else if(!added_as_client && bssid!=null){
            AP temp = AP.getAPByMac(bssid);
            if (temp != null){
                this.bssid = bssid;
                temp.addClient(this);
                added_as_client = true;
                connected++;
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
    void showInfo(FragmentManager fragmentManager){
        STDialog dialog = new STDialog();
        dialog.info_st = this;
        dialog.show(fragmentManager, "STDialog");
    }
    public String toString(){
        return mac + '\t' + (bssid==null ? "(not associated)" : bssid) + '\t' + pwr + '\t' + frames + '\t' + lost + '\t' + manuf + '\n';
    }
    static ST getSTByMac(String mac){
        for(int i=STs.size()-1;i>=0;i--){
            if(mac.equals(STs.get(i).mac)){
                return STs.get(i);
            }
        }
        return null;
    }
    static void clear(){
        STs.clear();
        connected = 0;
    }
}
