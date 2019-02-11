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

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_MDK_DOS;
import static com.hijacker.MainActivity.SORT_BEACONS_FRAMES;
import static com.hijacker.MainActivity.SORT_DATA_FRAMES;
import static com.hijacker.MainActivity.SORT_ESSID;
import static com.hijacker.MainActivity.SORT_NOSORT;
import static com.hijacker.MainActivity.SORT_PWR;
import static com.hijacker.MainActivity.adapter;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.aliases;
import static com.hijacker.MainActivity.aliases_file;
import static com.hijacker.MainActivity.aliases_in;
import static com.hijacker.MainActivity.cap_path;
import static com.hijacker.MainActivity.copy;
import static com.hijacker.MainActivity.data_path;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getFixed;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.isolate;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.sort;
import static com.hijacker.MainActivity.startAireplay;
import static com.hijacker.MainActivity.startAireplayWEP;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.MainActivity.stopWPA;
import static com.hijacker.MainActivity.target_deauth;
import static com.hijacker.MainActivity.toSort;
import static com.hijacker.MainActivity.wpa_runnable;
import static com.hijacker.MainActivity.wpa_thread;

class AP extends Device{
    static final int WPA=0, WPA2=1, WEP=2, OPN=3, UNKNOWN=4;
    static int wpa=0, wpa2=0, wep=0, opn=0, hidden=0;
    static final HashMap<String, AP> APsHM = new HashMap<>();
    static final ArrayList<AP> APs = new ArrayList<>();
    static final ArrayList<AP> marked = new ArrayList<>();
    static final ArrayList<AP> currentTargetDeauth = new ArrayList<>();
    boolean isHidden = false;
    int ch, id, sec=UNKNOWN;
    private int beacons, data, ivs, total_beacons=0, total_data=0, total_ivs=0;
    String essid, enc, cipher, auth;
    final ArrayList<ST> clients = new ArrayList<>();
    AP(String essid, String mac, String enc, String cipher, String auth,
       int pwr, int beacons, int data, int ivs, int ch) {
        super(mac);
        id = APs.size();
        this.update(essid, enc, cipher, auth, pwr, beacons, data, ivs, ch);

        upperRight = this.manuf;
        lowerLeft = this.mac;

        APs.add(this);
        APsHM.put(this.mac, this);
    }

    void addClient(ST client){
        this.clients.add(client);
        if(currentTargetDeauth.contains(this)){
            client.disconnect();
        }
    }
    void update(){
        //For refresh
        this.update(this.essid, this.enc, this.cipher, this.auth, this.pwr, this.beacons, this.data, this.ivs, this.ch);
    }
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

        if(beacons!=this.beacons || data!=this.data || ivs!=this.ivs || this.lastseen==0){
            this.lastseen = System.currentTimeMillis();
        }

        if(beacons<this.beacons || data<this.data || ivs<this.ivs){
            saveData();
        }else{
            this.beacons = beacons;
            this.data = data;
            this.ivs = ivs;
        }

        this.enc = enc;
        this.cipher = cipher;
        this.auth = auth;
        this.pwr = pwr;
        this.ch = ch;

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
        upperLeft = this.essid + (this.alias==null ? "" : " (" + alias + ')');
        lowerRight = "PWR: " + this.pwr + " | SEC: " + this.enc + " | CH: " + this.ch + " | B:" + this.getBeacons() + " | D:" + this.getData();
        runInHandler(new Runnable(){
            @Override
            public void run(){
                if(tile!=null) tile.update();
                else tile = new Tile(id, AP.this);

                if(toSort) Tile.sort();
            }
        });
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
            Airodump.reset();
            Airodump.setAP(this);
            Airodump.setForWEP(true);
            Airodump.start();
            if(!this.essid.equals("<hidden>")) startAireplayWEP(this);
            progress.setIndeterminate(true);
        }else if(this.sec == WPA || this.sec == WPA2){
            //wpa/wpa2
            stopWPA();
            if(debug) Log.d("HIJACKER/AP", "Cracking WPA/WPA2");
            Airodump.reset();
            Airodump.setAP(this);
            Airodump.setForWPA(true);
            Airodump.start();
            startAireplay(this.mac);
            wpa_thread = new Thread(wpa_runnable);
            wpa_thread.start();
        }
        if(is_ap==null) isolate(this.mac);
    }
    void crackReaver(MainActivity activity){
        FragmentManager fragmentManager = activity.getFragmentManager();

        ReaverFragment.ap = this;
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragment1, activity.reaverFragment.setAutostart(true));
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();      //Wait for everything to be set up
    }
    void disconnectAll(){
        if(Airodump.getChannel() != this.ch){
            if(debug) Log.d("HIJACKER/AP", "Starting airodump for channel " + this.ch);
            Airodump.startClean(this.ch);
            stop(PROCESS_AIREPLAY);         //Aireplay is useless since we are changing channel
        }
        if(target_deauth){
            if(debug) Log.d("HIJACKER/AP", "Starting targeted deauthentication for " + this.mac + "...");
            int i;
            for(i=0;i<this.clients.size();i++){
                this.clients.get(i).disconnect();
            }
            currentTargetDeauth.add(this);
        }else{
            if(debug) Log.d("HIJACKER/AP", "Starting aireplay without targets for " + this.mac + "...");
            startAireplay(this.mac);
        }
    }

    int getBeacons(){ return total_beacons + beacons; }
    int getData(){ return total_data + data; }
    int getIvs(){ return total_ivs + ivs; }

    static void clear(){
        APsHM.clear();
        APs.clear();
        marked.clear();
        wpa = 0;
        wpa2 = 0;
        wep = 0;
        opn = 0;
        hidden = 0;
    }
    static void saveAll(){
        for(AP ap : APs){
            ap.saveData();
        }
    }
    static AP getAPByMac(String mac){
        return APsHM.get(mac);
    }

    void showInfo(FragmentManager fragmentManager){
        APDialog dialog = new APDialog();
        dialog.info_ap = this;
        dialog.show(fragmentManager, "APDialog");
    }
    public String toString(){
        return this.essid + (this.alias==null ? "" : " (" + this.alias + ')') + " (" + this.mac + ')';
    }
    String getExported(){
        //MAC                 PWR  CH  Beacons    Data      #s   ENC  AUTH  CIPHER  Hidden  ESSID - Manufacturer
        //00:11:22:33:44:55  -100  13   123456  123456  123456  WPA2  TKIP    CCMP     Yes  ExampleESSID - ExampleManufacturer
        String str = mac;
        str += getFixed(Integer.toString(pwr), 6);
        str += getFixed(Integer.toString(ch), 4);
        str += getFixed(Integer.toString(getBeacons()), 9);
        str += getFixed(Integer.toString(getData()), 8);
        str += getFixed(Integer.toString(getIvs()), 8);
        str += getFixed(enc, 6);
        str += getFixed(auth, 6);
        str += getFixed(cipher, 8);
        str += getFixed(isHidden ? "Yes" : "No", 8);
        str += "  " + essid  + (alias==null ? "" : " (" + alias + ')') + " - " + manuf;

        return str;
    }
    void saveData(){
        total_beacons += beacons;
        total_data += data;
        total_ivs += ivs;
        beacons = 0;
        data = 0;
        ivs = 0;
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
    PopupMenu getPopupMenu(final MainActivity activity, final View v){
        PopupMenu popup = new PopupMenu(activity, v);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        //add(groupId, itemId, order, title)
        popup.getMenu().add(0, 0, 0, "Info");
        popup.getMenu().add(0, 1, 1, this.isMarked ? "Unmark" : "Mark");
        popup.getMenu().add(0, 2, 2, "Copy MAC");
        popup.getMenu().add(0, 3, 3, "Watch");
        popup.getMenu().add(0, 5, 4, "Set alias");
        popup.getMenu().add(0, 4, 5, "Attack...");

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(android.view.MenuItem item) {
                if(debug) Log.d("HIJACKER/MyListFragment", "Clicked " + item.getItemId() + " for ap");
                switch(item.getItemId()) {
                    case 0:
                        //Info
                        AP.this.showInfo(activity.getFragmentManager());
                        break;
                    case 1:
                        //mark or unmark
                        if(AP.this.isMarked){
                            AP.this.unmark();
                        }else{
                            AP.this.mark();
                        }
                        break;
                    case 2:
                        //copy mac to clipboard
                        copy(AP.this.mac, v);
                        break;
                    case 3:
                        //Watch
                        MainActivity.isolate(AP.this.mac);
                        Airodump.startClean(AP.this);
                        break;
                    case 4:
                        //attack
                        PopupMenu popup2 = new PopupMenu(activity, v);
                        popup2.getMenuInflater().inflate(R.menu.popup_menu, popup2.getMenu());

                        //add(groupId, itemId, order, title)
                        popup2.getMenu().add(0, 0, 0, "Disconnect...");
                        if(AP.this.clients.size()>0) popup2.getMenu().add(0, 1, 1, "Disconnect Client");
                        popup2.getMenu().add(0, 2, 2, "Copy disconnect command");

                        popup2.getMenu().add(0, 3, 3, "DoS");
                        if(AP.this.sec==WPA || AP.this.sec==WPA2 || AP.this.sec==WEP){
                            popup2.getMenu().add(0, 4, 4, "Crack");
                            popup2.getMenu().add(0, 5, 5, "Copy crack command");
                        }
                        popup2.getMenu().add(0, 6, 6, "Crack with Reaver");

                        popup2.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                            @Override
                            public boolean onMenuItemClick(MenuItem item){
                                switch(item.getItemId()){
                                    case 0:
                                        //Disconnect
                                        AP.this.disconnectAll();
                                        break;
                                    case 1:
                                        //Disconnect client
                                        PopupMenu popup = new PopupMenu(activity, v);

                                        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                                        for (int i = 0; i < AP.this.clients.size(); i++) {
                                            popup.getMenu().add(0, i, i, AP.this.clients.get(i).toString());
                                        }
                                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                            public boolean onMenuItemClick(android.view.MenuItem item) {
                                                //ItemId = i (in for())
                                                AP.this.clients.get(item.getItemId()).disconnect();
                                                return true;
                                            }
                                        });
                                        popup.show();
                                        break;
                                    case 2:
                                        //Copy disconnect command
                                        String str2 = prefix + " " + aireplay_dir + " --deauth 0 -a " + AP.this.mac + " " + iface;
                                        copy(str2, v);
                                        break;
                                    case 3:
                                        //DoS
                                        stop(PROCESS_MDK_DOS);
                                        MDKFragment.ados_ap = AP.this;
                                        FragmentTransaction ft = mFragmentManager.beginTransaction();
                                        ft.replace(R.id.fragment1, new MDKFragment());
                                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                                        ft.addToBackStack(null);
                                        ft.commitAllowingStateLoss();
                                        mFragmentManager.executePendingTransactions();
                                        MDKFragment.ados_switch.setChecked(true);
                                        break;
                                    case 4:
                                        //Crack
                                        AP.this.crack();
                                        break;
                                    case 5:
                                        //copy crack command
                                        String str;
                                        if(AP.this.sec==WEP) str = prefix + " " + airodump_dir + " --channel " + AP.this.ch + " --bssid " + AP.this.mac + " --ivs -w " + cap_path + "/wep_ivs " + iface;
                                        else str = prefix + " " + airodump_dir + " --channel " + AP.this.ch + " --bssid " + AP.this.mac + " -w " + cap_path + "/handshake " + iface;

                                        copy(str, v);
                                        break;
                                    case 6:
                                        //crack with reaver
                                        if(ReaverFragment.isRunning()){
                                            Toast.makeText(activity, activity.getString(R.string.reaver_already_running), Toast.LENGTH_SHORT).show();
                                        }else{
                                            AP.this.crackReaver(activity);
                                        }
                                        break;
                                }
                                return false;
                            }
                        });
                        popup2.show();
                        break;
                    case 5:
                        //Set alias
                        final EditTextDialog dialog = new EditTextDialog();
                        dialog.setTitle(activity.getString(R.string.set_alias));
                        dialog.setDefaultText(AP.this.alias);
                        dialog.setAllowEmpty(true);
                        dialog.setRunnable(new Runnable(){
                            @Override
                            public void run(){
                                if(dialog.result.equals("")) dialog.result = null;
                                try{
                                    if(AP.this.alias==null ^ dialog.result==null){
                                        //Need to remove previous alias
                                        File temp_aliases = new File(data_path + "/temp_aliases");
                                        if(temp_aliases.exists()) temp_aliases.delete();
                                        temp_aliases.createNewFile();
                                        PrintWriter temp_in = new PrintWriter(new FileWriter(temp_aliases));

                                        BufferedReader aliases_out = new BufferedReader(new FileReader(aliases_file));

                                        //Copy current aliases to temp file, except the one we are changing
                                        String buffer = aliases_out.readLine();
                                        while(buffer!=null){
                                            //Line format: 00:11:22:33:44:55 Alias
                                            if(buffer.charAt(17)==' ' && buffer.length()>18){
                                                String mac = buffer.substring(0, 17);
                                                String alias = buffer.substring(18);
                                                if(!mac.equals(AP.this.mac)){
                                                    temp_in.println(mac + ' ' + alias);
                                                }
                                            }else{
                                                Log.e("HIJACKER/setup", "Aliases file format error: " + buffer);
                                            }
                                            buffer = aliases_out.readLine();
                                        }
                                        temp_in.flush();
                                        temp_in.close();
                                        aliases_out.close();

                                        aliases_file.delete();
                                        temp_aliases.renameTo(aliases_file);
                                        aliases_in = new FileWriter(aliases_file, true);
                                    }
                                    if(dialog.result!=null){
                                        aliases_in.write(AP.this.mac + ' ' + dialog.result + '\n');
                                        aliases_in.flush();
                                    }
                                }catch(IOException e){
                                    Log.e("HIJACKER/MyListFrgm", e.toString());
                                }
                                aliases.put(AP.this.mac, dialog.result);
                                AP.this.alias = dialog.result;
                                AP.this.update();
                            }
                        });
                        dialog.show(activity.getFragmentManager(), "EditTextDialog");
                }
                return true;
            }
        });
        return popup;
    }
}
