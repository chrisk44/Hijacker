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
    static final ArrayList<Tile> tiles = new ArrayList<>();
    static final List<Tile> allTiles = new ArrayList<>();
    static int i=0;                                //End of APs in items
    Device device;
    boolean show=true;
    Tile(int index, Device dev){
        this.device = dev;
        allTiles.add(index, this);

        this.check();
        if(this.show){
            addToView();
        }
    }
    void update(){
        if(!this.show){
            this.check();
            if(this.show){
                addToView();
            }
        }else{
            this.check();
            if(!this.show){
                filter();
            }
        }
        adapter.notifyDataSetChanged();
    }
    void check(){
        if(is_ap==null) {
            if(device instanceof AP){
                AP ap = (AP)device;
                boolean channel = (ap.ch<0 || ap.ch>14) ? true : (show_ch[0] || show_ch[ap.ch]);    //Channel might be -1 or over 14 (5ghz) so avoid OutOfRangeException in array access
                this.show = show_ap && channel && ap.pwr>=pwr_filter*(-1) &&
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
    void addToView(){
        int index;
        if(this.device instanceof AP){
            index = i;
            if(i>0 && getComparatorForAP()!=null){
                index = findIndex(tiles.subList(0, i), this, getComparatorForAP());
            }
            i++;
        }else{
            index = tiles.size();
            if(tiles.size() - i > 0 && getComparatorForST()!=null){
                index = findIndex(tiles.subList(i, tiles.size()), this, getComparatorForST()) + i;
            }
        }
        tiles.add(index, this);
        adapter.insert(this, index);
        onCountsChanged();
    }
    static int findIndex(List<Tile> list, Tile tile, Comparator<Tile> comp){
        /*
            Returns the index of 'list' in which 'tile' should be added for the array to remain sorted according to 'comp'.
            Use binary search to find an item that 'tile' "equals" to (according to 'comp')
            If nothing is found, then the new item should be added at the index we were
            when we gave up on searching.
        */
        if(list.size()==0) return 0;
        Tile array[] = list.toArray(new Tile[list.size()]);
        int L = 0, R = list.size()-1, M = 0;
        while(L<=R){
            M = (L + R)/2;

            if(comp.compare(array[M], tile)==0) return M;
            else if(comp.compare(array[M], tile)<0) L = M + 1;
            else R = M - 1;
        }
        if(comp.compare(array[M], tile)>0) return M;
        else return M + 1;
    }
    static void clear(){
        allTiles.clear();
        tiles.clear();
        adapter.clear();
        AP.clear();
        ST.clear();
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
                temp.addToView();
            }
        }
        sort();
    }
    static void sort(){
        //sort allTiles 0/i for APs and i/allTiles.size() for STs
        if(sort!=SORT_NOSORT){
            if(debug) Log.d("HIJACKER/Tile", "Sorting: sort is " + sort + ", sort_reverse is " + sort_reverse);
            if(show_ap && i>0 && getComparatorForAP()!=null){
                Collections.sort(tiles.subList(0, i), getComparatorForAP());
            }
            if(show_st && tiles.size()-i > 0 && getComparatorForST()!=null){
                Collections.sort(tiles.subList(i, tiles.size()), getComparatorForST());
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

    static Comparator<Tile> AP_ESSID = new Comparator<Tile>(){
        @Override
        public int compare(Tile o1, Tile o2){
            if(sort_reverse) return ((AP)o2.device).essid.compareToIgnoreCase(((AP)o1.device).essid);
            else return ((AP)o1.device).essid.compareToIgnoreCase(((AP)o2.device).essid);
        }
    };
    static Comparator<Tile> AP_BEACONS = new Comparator<Tile>(){
        @Override
        public int compare(Tile o1, Tile o2){
            if(sort_reverse) return ((AP)o1.device).getBeacons() - ((AP)o2.device).getBeacons();
            else return ((AP)o2.device).getBeacons() - ((AP)o1.device).getBeacons();
        }
    };
    static Comparator<Tile> AP_DATA = new Comparator<Tile>(){
        @Override
        public int compare(Tile o1, Tile o2){
            if(sort_reverse) return ((AP)o1.device).getData() - ((AP)o2.device).getData();
            else return ((AP)o2.device).getData() - ((AP)o1.device).getData();
        }
    };
    static Comparator<Tile> ST_FRAMES = new Comparator<Tile>(){
        @Override
        public int compare(Tile o1, Tile o2){
            if(sort_reverse) return ((ST)o1.device).getFrames() - ((ST)o2.device).getFrames();
            else return ((ST)o2.device).getFrames() - ((ST)o1.device).getFrames();
        }
    };
    static Comparator<Tile> AP_ST_PWR = new Comparator<Tile>(){
        @Override
        public int compare(Tile o1, Tile o2){
            if(sort_reverse) return o1.device.pwr - o2.device.pwr;
            else return o2.device.pwr - o1.device.pwr;
        }
    };
    static Comparator<Tile> getComparatorForAP(){
        switch(sort){
            case SORT_ESSID:
                return AP_ESSID;
            case SORT_BEACONS_FRAMES:
                return AP_BEACONS;
            case SORT_DATA_FRAMES:
                return AP_DATA;
            case SORT_PWR:
                return AP_ST_PWR;
            default:
                return null;
        }
    }
    static Comparator<Tile> getComparatorForST(){
        switch(sort){
            case SORT_ESSID:
                return null;
            case SORT_BEACONS_FRAMES:
                return ST_FRAMES;
            case SORT_DATA_FRAMES:
                return ST_FRAMES;
            case SORT_PWR:
                return AP_ST_PWR;
            default:
                return null;
        }
    }
}
