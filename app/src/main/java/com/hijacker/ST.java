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
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import static com.hijacker.AP.getAPByMac;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.SORT_BEACONS_FRAMES;
import static com.hijacker.MainActivity.SORT_DATA_FRAMES;
import static com.hijacker.MainActivity.SORT_NOSORT;
import static com.hijacker.MainActivity.SORT_PWR;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.aliases;
import static com.hijacker.MainActivity.aliases_file;
import static com.hijacker.MainActivity.aliases_in;
import static com.hijacker.MainActivity.copy;
import static com.hijacker.MainActivity.data_path;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getFixed;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.sort;
import static com.hijacker.MainActivity.startAireplay;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.MainActivity.toSort;

class ST extends Device{
    static final HashMap<String, ST> STsHM = new HashMap<>();
    static final ArrayList<ST> STs = new ArrayList<>();
    static final ArrayList<ST> marked = new ArrayList<>();
    static String paired, not_connected;
    static int connected=0;     //Stations that are connected to an AP
    int id;
    private int frames, lost, total_frames=0, total_lost=0;
    AP connectedTo = null;
    String bssid, probes;
    ST(String mac, String bssid, int pwr, int lost, int frames, String probes){
        super(mac);
        this.id = STs.size();
        this.update(bssid, pwr, lost, frames, probes);

        upperRight = this.manuf;

        STs.add(this);
        STsHM.put(this.mac, this);
    }
    void disconnect(){
        if(Airodump.getChannel() != connectedTo.ch){
            //switch channel only if airodump is running elsewhere
            stop(PROCESS_AIREPLAY);
            Airodump.startClean(connectedTo.ch);
        }
        startAireplay(this.bssid, this.mac);
    }
    void update(){
        //For refresh
        this.update(this.bssid, this.pwr, this.lost, this.frames, this.probes);
    }
    void update(String bssid, int pwr, int lost, int frames, String probes){
        if(connectedTo!=null){
            if(!connectedTo.mac.equals(bssid)){
                //Connected to a different network
                connectedTo.removeClient(this);
                connectedTo = getAPByMac(bssid);
                if(connectedTo==null){
                    //Now not connected
                    connected--;
                    runInHandler(new Runnable(){
                        @Override
                        public void run(){
                            Tile.onCountsChanged();
                        }
                    });
                }else{
                    connectedTo.addClient(this);
                }
            }
        }else if(bssid!=null){
            //Now connected somewhere
            connectedTo = getAPByMac(bssid);
            if(connectedTo!=null){
                //Now connected to known AP
                connected++;
                connectedTo.addClient(this);
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        Tile.onCountsChanged();
                    }
                });
            }
        }
        if(frames!=this.frames || lost!=this.lost || this.lastseen==0){
            this.lastseen = System.currentTimeMillis();
        }
        if(!toSort && sort!=SORT_NOSORT){
            switch(sort){
                case SORT_BEACONS_FRAMES:
                    toSort = this.frames!=frames;
                    break;
                case SORT_DATA_FRAMES:
                    toSort = this.frames!=frames;
                    break;
                case SORT_PWR:
                    toSort = this.pwr!=pwr;
                    break;
            }
        }

        if(frames<this.frames || lost<this.lost){
            saveData();
        }else{
            this.lost = lost;
            this.frames = frames;
        }

        this.bssid = bssid;
        this.pwr = pwr;
        this.probes = probes.equals("") ? "No probes" : probes.replace(",", ", ");

        upperLeft = this.mac + (this.alias==null ? "" : " (" + alias + ')');
        if(connectedTo!=null){
            lowerLeft = paired + connectedTo.mac + " (" + connectedTo.essid + ")";
        }else lowerLeft = not_connected;
        lowerRight = "PWR: " + this.pwr + " | Frames: " + this.getFrames();
        runInHandler(new Runnable(){
            @Override
            public void run(){
                if(tile!=null) tile.update();
                else tile = new Tile(AP.APs.size() + id, ST.this);

                if(toSort) Tile.sort();
            }
        });
    }
    public int getFrames(){ return total_frames + frames; }
    public int getLost(){ return total_lost + lost; }
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

    static void clear(){
        STsHM.clear();
        STs.clear();
        marked.clear();
        connected = 0;
    }
    static void saveAll(){
        for(ST st : STs){
            st.saveData();
        }
    }
    static ST getSTByMac(String mac){
        return STsHM.get(mac);
    }

    void showInfo(FragmentManager fragmentManager){
        STDialog dialog = new STDialog();
        dialog.info_st = this;
        dialog.show(fragmentManager, "STDialog");
    }
    public String toString(){
        return this.mac + (this.alias==null ? "" : " (" + this.alias + ')') + ((this.bssid==null) ? "" : " (" + getAPByMac(this.bssid).essid + ')');
    }
    public String getExported(){
        //MAC                BSSID               PWR  Frames    Lost  Manufacturer - Probes
        //00:11:22:33:44:55  00:11:22:33:44:55  -100  123456  123456  ExampleManufacturer - Probe1, Probe2, Probe3...
        String str = mac;
        str += getFixed(bssid==null ? "(not associated) " : bssid, 19);
        str += getFixed(Integer.toString(pwr), 6);
        str += getFixed(Integer.toString(getFrames()), 8);
        str += getFixed(Integer.toString(getLost()), 8);
        str += "  " + manuf  + (alias==null ? "" : " (" + alias + ')') + " - " + probes;

        return str;
    }
    public void saveData(){
        total_frames += frames;
        total_lost += lost;
        frames = 0;
        lost = 0;
    }
    PopupMenu getPopupMenu(final MainActivity activity, final View v){
        PopupMenu popup = new PopupMenu(activity, v);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        popup.getMenu().add(0, 0, 0, "Info");
        popup.getMenu().add(0, 4, 1, ST.this.isMarked ? "Unmark" : "Mark");
        popup.getMenu().add(0, 1, 2, "Copy MAC");
        popup.getMenu().add(0, 5, 3, "Set alias");
        if(ST.this.bssid!=null){
            popup.getMenu().add(0, 2, 4, "Disconnect");
            popup.getMenu().add(0, 3, 5, "Copy disconnect command");
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(android.view.MenuItem item) {
                if(debug) Log.d("HIJACKER/MyListFragment", "Clicked " + item.getItemId() + " for st");
                switch(item.getItemId()) {
                    case 0:
                        //Info
                        ST.this.showInfo(activity.getFragmentManager());
                        break;
                    case 1:
                        //copy to clipboard
                        copy(ST.this.mac, v);
                        break;
                    case 2:
                        //Disconnect this
                        ST.this.disconnect();
                        break;
                    case 3:
                        //copy disconnect command to clipboard
                        String str = prefix + " " + aireplay_dir + " --ignore-negative-one --deauth 0 -a " + ST.this.bssid + " -c " + ST.this.mac + " " + iface;
                        copy(str, v);
                        break;
                    case 4:
                        //mark or unmark
                        if(ST.this.isMarked){
                            ST.this.unmark();
                        }else{
                            ST.this.mark();
                        }
                        break;
                    case 5:
                        //Set alias
                        final EditTextDialog dialog = new EditTextDialog();
                        dialog.setTitle(activity.getString(R.string.set_alias));
                        dialog.setAllowEmpty(true);
                        dialog.setDefaultText(ST.this.alias);
                        dialog.setRunnable(new Runnable(){
                            @Override
                            public void run(){
                                if(dialog.result.equals("")) dialog.result = null;
                                try{
                                    if(ST.this.alias==null ^ dialog.result==null){
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
                                                if(!mac.equals(ST.this.mac)){
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
                                        aliases_in.write(ST.this.mac + ' ' + dialog.result + '\n');
                                        aliases_in.flush();
                                    }
                                }catch(IOException e){
                                    Log.e("HIJACKER/MyListFrgm", e.toString());
                                }
                                aliases.put(ST.this.mac, dialog.result);
                                ST.this.alias = dialog.result;
                                ST.this.update();
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
