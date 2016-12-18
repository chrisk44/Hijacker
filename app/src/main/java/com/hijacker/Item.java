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

import java.util.ArrayList;
import java.util.List;

import static com.hijacker.AP.OPN;
import static com.hijacker.AP.UNKNOWN;
import static com.hijacker.AP.WEP;
import static com.hijacker.AP.WPA;
import static com.hijacker.AP.WPA2;
import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MainActivity.adapter;
import static com.hijacker.MainActivity.opn;
import static com.hijacker.MainActivity.pwr_filter;
import static com.hijacker.MainActivity.show_ap;
import static com.hijacker.MainActivity.show_ch;
import static com.hijacker.MainActivity.show_na_st;
import static com.hijacker.MainActivity.show_st;
import static com.hijacker.MainActivity.wep;
import static com.hijacker.MainActivity.wpa;

class Item {
    static int i=0;                                //End of APs in items
    static ArrayList<Item> items = new ArrayList<>();
    static List<Item> allItems = new ArrayList<>();
    AP ap=null;
    ST st=null;
    boolean type=true, show=true;        //type: true=AP, false=ST
    String s1, s2, s3, s4;
    Item(int index, String s1, String s2, String s3, String s4, boolean type, AP _ap, ST _st){
        this.type = type;
        this.ap = _ap;
        this.st = _st;
        allItems.add(index, this);

        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
        this.s4 = s4;

        this.check();
        if(this.show){
            if(type){
                items.add(i, this);
                adapter.insert(this, i);
                i++;
            }else{
                items.add(this);
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
                    items.add(i, this);
                    adapter.insert(this, i);
                    i++;
                }else{
                    items.add(this);
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
        allItems.clear();
        items.clear();
        adapter.clear();
        AP.clear();
        ST.clear();
        i=0;
    }
    static void filter(){
        items.clear();
        adapter.clear();
        i=0;
        Item temp;
        for(int j=0; j<allItems.size();j++){
            temp = allItems.get(j);
            temp.check();
            if(temp.show){
                if(temp.type){
                    items.add(i, temp);
                    adapter.insert(temp, i);
                    i++;
                }else{
                    items.add(temp);
                    adapter.add(temp);
                }
            }
        }
        MainActivity.refreshHandler.obtainMessage().sendToTarget();
    }
}
