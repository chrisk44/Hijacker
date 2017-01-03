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
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.opn;
import static com.hijacker.MainActivity.pwr_filter;
import static com.hijacker.MainActivity.show_ap;
import static com.hijacker.MainActivity.show_ch;
import static com.hijacker.MainActivity.show_na_st;
import static com.hijacker.MainActivity.show_st;
import static com.hijacker.MainActivity.sort;
import static com.hijacker.MainActivity.sort_reverse;
import static com.hijacker.MainActivity.toSort;
import static com.hijacker.MainActivity.wep;
import static com.hijacker.MainActivity.wpa;

class Tile {
    static int i=0;                                //End of APs in items
    static ArrayList<Tile> tiles = new ArrayList<>();
    static List<Tile> allTiles = new ArrayList<>();
    AP ap=null;
    ST st=null;
    boolean type=true, show=true;        //type: true=AP, false=ST
    String s1, s2, s3, s4;
    Tile(int index, String s1, String s2, String s3, String s4, boolean type, AP _ap, ST _st){
        this.type = type;
        this.ap = _ap;
        this.st = _st;
        allTiles.add(index, this);

        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
        this.s4 = s4;

        this.check();
        if(this.show){
            if(type){
                tiles.add(i, this);
                adapter.insert(this, i);
                i++;
            }else{
                tiles.add(this);
                adapter.add(this);
            }
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
                if(type){
                    tiles.add(i, this);
                    adapter.insert(this, i);
                    i++;
                }else{
                    tiles.add(this);
                    adapter.add(this);
                }
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
            if(type){
                //ap
                this.show = show_ap && (show_ch[0] || show_ch[this.ap.ch]) && this.ap.pwr>=pwr_filter*(-1) &&
                        ((wpa && (this.ap.sec == WPA || this.ap.sec == WPA2)) || (wep && this.ap.sec == WEP) || (opn && this.ap.sec == OPN) || this.ap.sec==UNKNOWN);
            } else this.show = show_st && (show_na_st || this.st.bssid != null) && this.st.pwr>=pwr_filter*(-1); //st
        }else{
            if(type){
                this.show = false;
            }else{
                this.show = is_ap.mac.equals(this.st.bssid) && show_st && this.st.pwr>=pwr_filter*(-1);
            }
        }
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
                if(temp.type){
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
                                if(sort_reverse) return o2.ap.essid.compareToIgnoreCase(o1.ap.essid);
                                else return o1.ap.essid.compareToIgnoreCase(o2.ap.essid);
                            }
                        });
                    }
                    break;
                case SORT_BEACONS_FRAMES:
                    if(show_ap && ap_sublist!=null){
                        Collections.sort(ap_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return o1.ap.beacons - o2.ap.beacons;
                                else return o2.ap.beacons - o1.ap.beacons;
                            }
                        });
                    }
                    if(show_st && st_sublist!=null){
                        Collections.sort(st_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return o1.st.frames - o2.st.frames;
                                else return o2.st.frames - o1.st.frames;
                            }
                        });
                    }
                    break;
                case SORT_DATA_FRAMES:
                    if(show_ap && ap_sublist!=null){
                        Collections.sort(ap_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return o1.ap.data - o2.ap.data;
                                else return o2.ap.data - o1.ap.data;
                            }
                        });
                    }
                    if(show_st && st_sublist!=null){
                        Collections.sort(st_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return o1.st.frames - o2.st.frames;
                                else return o2.st.frames - o1.st.frames;
                            }
                        });
                    }
                    break;
                case SORT_PWR:
                    if(show_ap && ap_sublist!=null){
                        Collections.sort(ap_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return o1.ap.pwr - o2.ap.pwr;
                                else return o2.ap.pwr - o1.ap.pwr;
                            }
                        });
                    }
                    if(show_st && st_sublist!=null){
                        Collections.sort(st_sublist, new Comparator<Tile>(){
                            @Override
                            public int compare(Tile o1, Tile o2){
                                if(sort_reverse) return o1.st.pwr - o2.st.pwr;
                                else return o2.st.pwr - o1.st.pwr;
                            }
                        });
                    }
                    break;
            }
        }
        toSort = false;
        MainActivity.refreshHandler.obtainMessage().sendToTarget();
    }
}
