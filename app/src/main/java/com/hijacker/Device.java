package com.hijacker;

/*
    Copyright (C) 2017  Christos Kyriakopoylos

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

import android.app.Activity;
import android.app.FragmentManager;
import android.view.View;
import android.widget.PopupMenu;

import static com.hijacker.MainActivity.SORT_NOSORT;
import static com.hijacker.MainActivity.aliases;
import static com.hijacker.MainActivity.getManuf;
import static com.hijacker.MainActivity.sort;
import static com.hijacker.MainActivity.toSort;

abstract class Device{
    String mac, manuf, alias;
    int pwr;
    long lastseen = 0;
    boolean isMarked = false;
    //UI tile
    Tile tile;
    String upperLeft, upperRight, lowerLeft, lowerRight;
    Device(String mac){
        this.mac = mac;
        this.manuf = getManuf(this.mac);
        this.alias = aliases.get(this.mac);
        if(sort!=SORT_NOSORT) toSort = true;
    }
    public abstract String toString();
    abstract String getExported();
    abstract void showInfo(FragmentManager fragmentManager);
    abstract void saveData();
    abstract void mark();
    abstract void unmark();
    abstract PopupMenu getPopupMenu(final Activity activity, final View v);

    static long toLong(String mac){
        if(mac==null) return -1;
        return Long.decode("0x" + mac.replace(":", "").toLowerCase());
    }
}
