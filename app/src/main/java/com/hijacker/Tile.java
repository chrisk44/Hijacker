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

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.hijacker.AP.OPN;
import static com.hijacker.AP.UNKNOWN;
import static com.hijacker.AP.WEP;
import static com.hijacker.AP.WPA;
import static com.hijacker.AP.WPA2;
import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MainActivity.SORT_BEACONS_FRAMES;
import static com.hijacker.MainActivity.SORT_DATA_FRAMES;
import static com.hijacker.MainActivity.SORT_ESSID;
import static com.hijacker.MainActivity.SORT_NOSORT;
import static com.hijacker.MainActivity.SORT_PWR;
import static com.hijacker.MainActivity.adapter;
import static com.hijacker.MainActivity.ap_count;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.manuf_filter;
import static com.hijacker.MainActivity.notification;
import static com.hijacker.MainActivity.opn;
import static com.hijacker.MainActivity.pwr_filter;
import static com.hijacker.MainActivity.show_ap;
import static com.hijacker.MainActivity.show_ch;
import static com.hijacker.MainActivity.show_na_st;
import static com.hijacker.MainActivity.show_st;
import static com.hijacker.MainActivity.sort;
import static com.hijacker.MainActivity.sort_reverse;
import static com.hijacker.MainActivity.st_count;
import static com.hijacker.MainActivity.toSort;
import static com.hijacker.MainActivity.wep;
import static com.hijacker.MainActivity.wpa;

class Tile {
    static int i=0;                                //End of APs in items
    static final ArrayList<Tile> tiles = new ArrayList<>();
    static final List<Tile> allTiles = new ArrayList<>();
    Device device;
    boolean show=true;
    String s1, s2, s3, s4;
    Tile(int index, String s1, String s2, String s3, String s4, Device dev){
        this.device = dev;
        allTiles.add(index, this);

        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
        this.s4 = s4;

        this.check();
        if(this.show){
            if(device instanceof AP){
                tiles.add(i, this);
                adapter.insert(this, i);
                i++;
            }else{
                tiles.add(this);
                adapter.add(this);
            }
            onCountsChanged();
        }
    }
    void update(String s1, String s2, String s3, String s4){
        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
        this.s4 = s4;
        if(!this.show){
            this.check();
            if(this.show){
                if(device instanceof AP){
                    tiles.add(i, this);
                    adapter.insert(this, i);
                    i++;
                }else{
                    tiles.add(this);
                    adapter.add(this);
                }
                onCountsChanged();
            }
        }else{
            this.check();
            if(!this.show){
                filter();
            }
        }
    }
    void check(){
        if(is_ap==null) {
            if(device instanceof AP){
                AP ap = (AP)device;
                this.show = show_ap && (show_ch[0] || show_ch[ap.ch]) && ap.pwr>=pwr_filter*(-1) &&
                        ((wpa && (ap.sec == WPA || ap.sec == WPA2)) || (wep && ap.sec == WEP) ||
                                (opn && ap.sec == OPN) || ap.sec==UNKNOWN) && ap.manuf.contains(manuf_filter);
            }else{
                ST st = (ST)device;
                this.show = show_st && (show_na_st || st.bssid != null) && st.pwr>=pwr_filter*(-1)  &&
                        st.manuf.contains(manuf_filter); //st
            }
        }else{
            if(device instanceof AP){
                this.show = false;
            }else{
                ST st = (ST)device;
                this.show = is_ap.mac.equals(st.bssid) && show_st && st.pwr>=pwr_filter*(-1) && st.manuf.contains(manuf_filter);
            }
        }
    }
    static void clear(){
        allTiles.clear();
        tiles.clear();
        adapter.clear();
        AP.clear();
        ST.clear();
        Device.clear();
        i=0;
    }
    static void filter(){
        if(debug) Log.d("HIJACKER/Tile", "Filtering...");
        tiles.clear();
        adapter.clear();
        i=0;
        Tile temp;
        for(int j=0; j<allTiles.size();j++){
            temp = allTiles.get(j);
            temp.check();
            if(temp.show){
                if(temp.device instanceof AP){
                    tiles.add(i, temp);
                    adapter.insert(temp, i);
                    i++;
                }else{
                    tiles.add(temp);
                    adapter.add(temp);
                }
            }
        }
        sort();
        onCountsChanged();
    }
    static void sort(){
        //sort allTiles 0/i for APs and i/allTiles.size() for STs
        if(sort!=SORT_NOSORT){
            if(debug) Log.d("HIJACKER/Tile", "Sorting: sort is " + sort + ", sort_reverse is " + sort_reverse);
            List<Tile> ap_sublist = null;
            List<Tile> st_sublist = null;
            if(i>0) ap_sublist = tiles.subList(0, i);
            if(tiles.size() - i > 0) st_sublist = tiles.subList(i, tiles.size());
            switch(sort){
                case SORT_ESSID:
                    if(show_ap && ap_sublist!=null){
                        Collections.sort(ap_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return ((AP)o2.device).essid.compareToIgnoreCase(((AP)o1.device).essid);
                                else return ((AP)o1.device).essid.compareToIgnoreCase(((AP)o2.device).essid);
                            }
                        });
                    }
                    break;
                case SORT_BEACONS_FRAMES:
                    if(show_ap && ap_sublist!=null){
                        Collections.sort(ap_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return ((AP)o1.device).getBeacons() - ((AP)o2.device).getBeacons();
                                else return ((AP)o2.device).getBeacons() - ((AP)o1.device).getBeacons();
                            }
                        });
                    }
                    if(show_st && st_sublist!=null){
                        Collections.sort(st_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return ((ST)o1.device).getFrames() - ((ST)o2.device).getFrames();
                                else return ((ST)o2.device).getFrames() - ((ST)o1.device).getFrames();
                            }
                        });
                    }
                    break;
                case SORT_DATA_FRAMES:
                    if(show_ap && ap_sublist!=null){
                        Collections.sort(ap_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return ((AP)o1.device).getData() - ((AP)o2.device).getData();
                                else return ((AP)o2.device).getData() - ((AP)o1.device).getData();
                            }
                        });
                    }
                    if(show_st && st_sublist!=null){
                        Collections.sort(st_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return((ST)o1.device).getFrames() - ((ST)o2.device).getFrames();
                                else return ((ST)o2.device).getFrames() - ((ST)o1.device).getFrames();
                            }
                        });
                    }
                    break;
                case SORT_PWR:
                    if(show_ap && ap_sublist!=null){
                        Collections.sort(ap_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return o1.device.pwr - o2.device.pwr;
                                else return o2.device.pwr - o1.device.pwr;
                            }
                        });
                    }
                    if(show_st && st_sublist!=null){
                        Collections.sort(st_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return o1.device.pwr - o2.device.pwr;
                                else return o2.device.pwr - o1.device.pwr;
                            }
                        });
                    }
                    break;
            }
        }
        toSort = false;
        adapter.notifyDataSetChanged();
    }
    static void onCountsChanged(){
        ap_count.setText(Integer.toString(is_ap==null ? Tile.i : 1));
        st_count.setText(Integer.toString(Tile.tiles.size() - Tile.i));
        if(StatsDialog.isResumed){
            StatsDialog.runnable.run();
        }
        notification();
    }
}
